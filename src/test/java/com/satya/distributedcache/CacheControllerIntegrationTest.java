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
class CacheControllerIntegrationTest {

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

    @Test
    void health() throws Exception {
        mvc.perform(get(path("/health"))).andExpect(status().isOk());
    }

    @Test
    void get_missing_returns_404() throws Exception {
        mvc.perform(get(path("/no_such_key"))).andExpect(status().isNotFound());
    }

    @Test
    void set_get_delete_exists_flow_with_string() throws Exception {
        String key = "k1";

        // exists=false initially
        MvcResult exists0 = mvc.perform(get(path("/exists/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(exists0.getResponse().getContentAsString()).isEqualTo("false");

        // set string via JSON
        mvc.perform(post(path("/" + key))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"bar\""))
                .andExpect(status().isOk());

        // get returns the same string
        MvcResult getResp = mvc.perform(get(path("/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(getResp.getResponse().getContentAsString()).isEqualTo("bar");

        // exists=true after set
        MvcResult exists1 = mvc.perform(get(path("/exists/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(exists1.getResponse().getContentAsString()).isEqualTo("true");

        // delete
        mvc.perform(delete(path("/" + key)))
                .andExpect(status().isNoContent());

        // get now 404
        mvc.perform(get(path("/" + key)))
                .andExpect(status().isNotFound());

        // exists=false after delete
        MvcResult exists2 = mvc.perform(get(path("/exists/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(exists2.getResponse().getContentAsString()).isEqualTo("false");
    }

    @Test
    void expire_and_ttl_behaviour_minimal() throws Exception {
        String key = "ttlKey";

        // set a JSON object
        mvc.perform(post(path("/" + key))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"v\":1}"))
                .andExpect(status().isOk());

        // set TTL 500ms
        mvc.perform(post(path("/expire/" + key + "/500")))
                .andExpect(status().isOk());

        // ttl should be >= 0 and <= 500 initially
        MvcResult ttl0 = mvc.perform(get(path("/ttl/" + key)))
                .andExpect(status().isOk()).andReturn();
        long ttlVal = Long.parseLong(ttl0.getResponse().getContentAsString());
        assertThat(ttlVal).isBetween(0L, 500L);

        // after ~600ms, key should be gone
        Thread.sleep(600);
        mvc.perform(get(path("/" + key))).andExpect(status().isNotFound());

        // ttl returns -2 for missing key
        MvcResult ttlAfter = mvc.perform(get(path("/ttl/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(Long.parseLong(ttlAfter.getResponse().getContentAsString())).isEqualTo(-2L);
    }

    @Test
    void set_get_with_text_plain() throws Exception {
        String key = "txt1";
        mvc.perform(post(path("/" + key))
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isOk());

        MvcResult resp = mvc.perform(get(path("/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(resp.getResponse().getContentAsString()).isEqualTo("hello");
    }

    @Test
    void ttl_no_ttl_and_delete_clears_ttl() throws Exception {
        String key = "noTtl";
        mvc.perform(post(path("/" + key))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"val\""))
                .andExpect(status().isOk());

        MvcResult ttl0 = mvc.perform(get(path("/ttl/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(Long.parseLong(ttl0.getResponse().getContentAsString())).isEqualTo(-1L);

        mvc.perform(post(path("/expire/" + key + "/1000")))
                .andExpect(status().isOk());
        mvc.perform(delete(path("/" + key))).andExpect(status().isNoContent());

        MvcResult ttlAfterDel = mvc.perform(get(path("/ttl/" + key)))
                .andExpect(status().isOk()).andReturn();
        assertThat(Long.parseLong(ttlAfterDel.getResponse().getContentAsString())).isEqualTo(-2L);
    }

    @Test
    void expire_missing_and_zero_immediate() throws Exception {
        String missing = "nope";
        MvcResult exp = mvc.perform(post(path("/expire/" + missing + "/123")))
                .andExpect(status().isOk()).andReturn();
        assertThat(exp.getResponse().getContentAsString()).isEqualTo("false");

        String k = "imm";
        mvc.perform(post(path("/" + k))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"x\""))
                .andExpect(status().isOk());

        mvc.perform(post(path("/expire/" + k + "/0")))
                .andExpect(status().isOk());

        // immediate expiration
        mvc.perform(get(path("/" + k))).andExpect(status().isNotFound());
        MvcResult ttl = mvc.perform(get(path("/ttl/" + k)))
                .andExpect(status().isOk()).andReturn();
        assertThat(Long.parseLong(ttl.getResponse().getContentAsString())).isEqualTo(-2L);

        MvcResult exists = mvc.perform(get(path("/exists/" + k)))
                .andExpect(status().isOk()).andReturn();
        assertThat(exists.getResponse().getContentAsString()).isEqualTo("false");
    }

}

