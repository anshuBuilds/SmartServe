package com.smartserve.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

class GuestRateLimitFilterTest {

    private GuestRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new GuestRateLimitFilter();
        ReflectionTestUtils.setField(filter, "limit", 1);
    }

    @Test
    void onlyGuestApiPathsAreRateLimited() {
        MockHttpServletRequest guest = new MockHttpServletRequest("GET", "/api/guest/menu/token");
        MockHttpServletRequest staff = new MockHttpServletRequest("GET", "/api/orders");

        assertFalse(filter.shouldNotFilter(guest));
        assertTrue(filter.shouldNotFilter(staff));
    }

    @Test
    void requestWithinLimitContinuesFilterChain() throws Exception {
        MockHttpServletRequest request = guestRequest("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertTrue(chain.getRequest() == request);
    }

    @Test
    void requestAboveLimitReturns429WithoutCallingEndpoint() throws Exception {
        filter.doFilter(guestRequest("10.0.0.1"), new MockHttpServletResponse(), new MockFilterChain());
        MockHttpServletResponse rejected = new MockHttpServletResponse();
        MockFilterChain rejectedChain = new MockFilterChain();

        filter.doFilter(guestRequest("10.0.0.1"), rejected, rejectedChain);

        assertEquals(429, rejected.getStatus());
        assertEquals("application/json", rejected.getContentType());
        assertTrue(rejected.getContentAsString().contains("Too many guest requests"));
        assertEquals(null, rejectedChain.getRequest());
    }

    @Test
    void separateClientAddressesHaveIndependentLimits() throws Exception {
        MockFilterChain firstChain = new MockFilterChain();
        MockFilterChain secondChain = new MockFilterChain();

        filter.doFilter(guestRequest("10.0.0.1"), new MockHttpServletResponse(), firstChain);
        filter.doFilter(guestRequest("10.0.0.2"), new MockHttpServletResponse(), secondChain);

        assertTrue(firstChain.getRequest() != null);
        assertTrue(secondChain.getRequest() != null);
    }

    private MockHttpServletRequest guestRequest(String remoteAddress) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/guest/menu/token");
        request.setRemoteAddr(remoteAddress);
        return request;
    }
}
