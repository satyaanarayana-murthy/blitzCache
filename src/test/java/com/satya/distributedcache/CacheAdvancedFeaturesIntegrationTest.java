package com.satya.distributedcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CacheAdvancedFeaturesIntegrationTest {

    @Autowired
    WebApplicationContext wac;

    MockMvc mvc;

    @BeforeEach
    void setup() {
        this.mvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private String path(String p) {
        return "/api/v1" + p;
    }

    private int parseSize(String stats) {
        // Example: "MapCache Stats: [size=0, maxSize=2000000, LRU Strategy Stats: ...]"
        int idx = stats.indexOf("size=");
        int comma = stats.indexOf(',', idx + 5);
        String num = stats.substring(idx + 5, comma).trim();
        return Integer.parseInt(num);
    }

    @Test
    void stats_endpoint_returns_string() throws Exception {
        MvcResult res = mvc.perform(get(path("/stats")))
                .andExpect(status().isOk()).andReturn();
        String stats = res.getResponse().getContentAsString();
        assertThat(stats).contains("MapCache Stats:");
    }

    @Test
    void ttl_purger_expires_without_reads_and_stats_reflects_cleanup() throws Exception {
        // Baseline size
        int baseSize = parseSize(mvc.perform(get(path("/stats")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        String key = "purge1";
        mvc.perform(post(path("/" + key))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"v\""))
                .andExpect(status().isOk());

        // size should increase by 1
        int afterPut = parseSize(mvc.perform(get(path("/stats")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(afterPut).isEqualTo(baseSize + 1);

        // set TTL 200ms and do not read the key
        mvc.perform(post(path("/expire/" + key + "/200")))
                .andExpect(status().isOk());

        Thread.sleep(350);

        // size should be back to baseline due to background purger
        int afterExpire = parseSize(mvc.perform(get(path("/stats")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        assertThat(afterExpire).isEqualTo(baseSize);
    }

    @Test
    void conditional_nx_xx_with_ttl() throws Exception {
        String k1 = "cond_nx";
        String k2 = "cond_xx";

        // Ensure keys start clean
        mvc.perform(delete(path("/" + k1))).andReturn();
        mvc.perform(delete(path("/" + k2))).andReturn();

        // NX (xx=false) when missing → true
        MvcResult nx1 = mvc.perform(post(path("/conditional/" + k1 + "/false/500"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"v1\""))
                .andExpect(status().isOk()).andReturn();
        assertThat(nx1.getResponse().getContentAsString()).isEqualTo("true");

        // NX again when exists → false (should not overwrite)
        MvcResult nx2 = mvc.perform(post(path("/conditional/" + k1 + "/false/500"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"v2\""))
                .andExpect(status().isOk()).andReturn();
        assertThat(nx2.getResponse().getContentAsString()).isEqualTo("false");

        // Value remains v1
        MvcResult get1 = mvc.perform(get(path("/" + k1)))
                .andExpect(status().isOk()).andReturn();
        assertThat(get1.getResponse().getContentAsString()).isEqualTo("v1");

        // XX (xx=true) when missing → false
        MvcResult xx1 = mvc.perform(post(path("/conditional/" + k2 + "/true/500"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"z1\""))
                .andExpect(status().isOk()).andReturn();
        assertThat(xx1.getResponse().getContentAsString()).isEqualTo("false");

        // Seed k2 normally
        mvc.perform(post(path("/" + k2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"z0\""))
                .andExpect(status().isOk());

        // XX when exists → true and update value
        MvcResult xx2 = mvc.perform(post(path("/conditional/" + k2 + "/true/500"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"z2\""))
                .andExpect(status().isOk()).andReturn();
        assertThat(xx2.getResponse().getContentAsString()).isEqualTo("true");

        MvcResult get2 = mvc.perform(get(path("/" + k2)))
                .andExpect(status().isOk()).andReturn();
        assertThat(get2.getResponse().getContentAsString()).isEqualTo("z2");

        // after TTL elapses, key should be gone
        Thread.sleep(600);
        mvc.perform(get(path("/" + k2))).andExpect(status().isNotFound());
    }

    @Test
    void expireAt_past_expires_immediately_and_future_sets_ttl() throws Exception {
        String k = "expat1";
        mvc.perform(post(path("/" + k))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"x\""))
                .andExpect(status().isOk());

        long now = System.currentTimeMillis();
        // Past → expire now (should return true and key gone)
        MvcResult past = mvc.perform(post(path("/expireAt/" + k + "/" + (now - 100))))
                .andExpect(status().isOk()).andReturn();
        // We expect true to indicate action taken
        assertThat(past.getResponse().getContentAsString()).isEqualTo("true");
        mvc.perform(get(path("/" + k))).andExpect(status().isNotFound());

        // Future → set new value and future expire
        String k2 = "expat2";
        mvc.perform(post(path("/" + k2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"y\""))
                .andExpect(status().isOk());
        long future = System.currentTimeMillis() + 400;
        MvcResult fut = mvc.perform(post(path("/expireAt/" + k2 + "/" + future)))
                .andExpect(status().isOk()).andReturn();
        assertThat(fut.getResponse().getContentAsString()).isEqualTo("true");
        Thread.sleep(550);
        mvc.perform(get(path("/" + k2))).andExpect(status().isNotFound());
    }

    @Test
    void persist_removes_ttl_but_keeps_value() throws Exception {
        String k = "persist1";
        mvc.perform(post(path("/" + k))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"v\""))
                .andExpect(status().isOk());

        mvc.perform(post(path("/expire/" + k + "/200")))
                .andExpect(status().isOk());

        // Persist should return true, keep value and make ttl=-1
        MvcResult res = mvc.perform(post(path("/persist/" + k)))
                .andExpect(status().isOk()).andReturn();
        assertThat(res.getResponse().getContentAsString()).isEqualTo("true");

        MvcResult ttl = mvc.perform(get(path("/ttl/" + k)))
                .andExpect(status().isOk()).andReturn();
        assertThat(Long.parseLong(ttl.getResponse().getContentAsString())).isEqualTo(-1L);

        MvcResult get = mvc.perform(get(path("/" + k)))
                .andExpect(status().isOk()).andReturn();
        assertThat(get.getResponse().getContentAsString()).isEqualTo("v");
    }

    @Test
    void updating_ttl_should_reschedule_purger_not_delete_at_old_time() throws Exception {
        String k = "retime1";
        mvc.perform(post(path("/" + k))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"v\""))
                .andExpect(status().isOk());

        // initial ttl 200ms
        mvc.perform(post(path("/expire/" + k + "/200")))
                .andExpect(status().isOk());

        // update to 1500ms before the 200ms window elapses
        Thread.sleep(100);
        mvc.perform(post(path("/expire/" + k + "/1500")))
                .andExpect(status().isOk());

        // wait past the first 200ms but before the 1500ms
        Thread.sleep(200);
        // key should still exist if purger honors the latest ttl
        mvc.perform(get(path("/" + k))).andExpect(status().isOk());
    }
}

