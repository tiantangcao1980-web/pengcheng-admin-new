package com.pengcheng.realty.customer.service;

import com.pengcheng.realty.customer.entity.Customer;
import com.pengcheng.realty.customer.mapper.RealtyCustomerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CustomerPoolService")
class CustomerPoolServiceTest {

    private RealtyCustomerMapper customerMapper;
    private CustomerPoolService service;

    @BeforeEach
    void setUp() {
        customerMapper = mock(RealtyCustomerMapper.class);
        service = new CustomerPoolService(customerMapper);
    }

    @Test
    @DisplayName("updateRecycleConfig 和 shouldRecycle 反映最新回收规则")
    void updateConfigAndShouldRecycleWork() {
        service.updateRecycleConfig(10, 40);

        Customer privateReported = customer(1L, 2, 1);
        privateReported.setCreateTime(LocalDateTime.now().minusDays(41));
        privateReported.setLastFollowTime(LocalDateTime.now().minusDays(11));

        assertThat(service.getNoFollowDays()).isEqualTo(10);
        assertThat(service.getNoVisitDays()).isEqualTo(40);
        assertThat(CustomerPoolService.shouldRecycle(privateReported, LocalDateTime.now(), 10, 40)).isTrue();
    }

    @Test
    @DisplayName("claimFromPublicPool 将客户转为私海并重置保护期")
    void claimFromPublicPoolClaimsCustomer() {
        Customer customer = customer(2001L, CustomerPoolService.POOL_PUBLIC, 1);
        when(customerMapper.selectById(2001L)).thenReturn(customer);

        service.claimFromPublicPool(2001L, 88L);

        assertThat(customer.getPoolType()).isEqualTo(CustomerPoolService.POOL_PRIVATE);
        assertThat(customer.getCreatorId()).isEqualTo(88L);
        assertThat(customer.getProtectionExpireTime()).isNotNull();
        assertThat(customer.getLastFollowTime()).isNotNull();
        verify(customerMapper).updateById(customer);
    }

    @Test
    @DisplayName("claimFromPublicPool 对非公海客户拒绝领取")
    void claimFromPublicPoolRejectsNonPublicCustomer() {
        Customer customer = customer(3001L, CustomerPoolService.POOL_PRIVATE, 1);
        when(customerMapper.selectById(3001L)).thenReturn(customer);

        assertThatThrownBy(() -> service.claimFromPublicPool(3001L, 88L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("不在公海池");
    }

    private Customer customer(Long id, int poolType, int status) {
        Customer customer = Customer.builder()
                .poolType(poolType)
                .status(status)
                .build();
        customer.setId(id);
        customer.setCreateTime(LocalDateTime.now().minusDays(50));
        customer.setLastFollowTime(LocalDateTime.now().minusDays(20));
        return customer;
    }
}
