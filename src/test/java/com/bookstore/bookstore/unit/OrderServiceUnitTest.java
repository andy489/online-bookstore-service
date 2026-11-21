package com.bookstore.bookstore.unit;

import com.bookstore.bookstore.domain.*;
import com.bookstore.bookstore.dto.request.UpdateOrderStatusRequest;
import com.bookstore.bookstore.dto.response.OrderResponse;
import com.bookstore.bookstore.exception.InvalidOperationException;
import com.bookstore.bookstore.repository.OrderRepository;
import com.bookstore.bookstore.repository.UserRepository;
import com.bookstore.bookstore.service.OrderPlacer;
import com.bookstore.bookstore.service.OrderService;
import com.bookstore.bookstore.service.OrderStatusUpdater;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceUnitTest {

    @Mock OrderRepository orderRepository;
    @Mock UserRepository userRepository;
    @Mock OrderPlacer orderPlacer;
    @Mock OrderStatusUpdater orderStatusUpdater;

    OrderService orderService;

    private User user;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, userRepository,
                orderPlacer, orderStatusUpdater);
        user = new User("user@test.com", "hash", "Test", "User", Role.USER);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
    }

    @Test
    void placeOrder_delegatesToOrderPlacer() {
        OrderResponse expected = mock(OrderResponse.class);
        when(orderPlacer.place(user)).thenReturn(expected);

        OrderResponse response = orderService.placeOrder("user@test.com");

        assertThat(response).isSameAs(expected);
        verify(orderPlacer).place(user);
    }

    @Test
    void placeOrder_retriesOnOptimisticLock_succeedsOnSecondAttempt() {
        OrderResponse expected = mock(OrderResponse.class);
        when(orderPlacer.place(user))
                .thenThrow(new ObjectOptimisticLockingFailureException("Book", null))
                .thenReturn(expected);

        OrderResponse response = orderService.placeOrder("user@test.com");

        assertThat(response).isSameAs(expected);
        verify(orderPlacer, times(2)).place(user);
    }

    @Test
    void placeOrder_exhaustsRetries_throwsInvalidOperation() {
        when(orderPlacer.place(user))
                .thenThrow(new ObjectOptimisticLockingFailureException("Book", null));

        assertThatThrownBy(() -> orderService.placeOrder("user@test.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Could not complete your order");
    }

    @Test
    void placeOrder_emptyCart_throwsInvalidOperation() {
        when(orderPlacer.place(user))
                .thenThrow(new InvalidOperationException("Cannot place order with an empty cart"));

        assertThatThrownBy(() -> orderService.placeOrder("user@test.com"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void updateStatus_delegatesToOrderStatusUpdater() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.SHIPPED);
        OrderResponse expected = mock(OrderResponse.class);
        when(orderStatusUpdater.update(1L, request)).thenReturn(expected);

        OrderResponse response = orderService.updateStatus(1L, request);

        assertThat(response).isSameAs(expected);
        verify(orderStatusUpdater).update(1L, request);
    }

    @Test
    void updateStatus_retriesOnOptimisticLock_succeedsOnSecondAttempt() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.SHIPPED);
        OrderResponse expected = mock(OrderResponse.class);
        when(orderStatusUpdater.update(anyLong(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Order", null))
                .thenReturn(expected);

        OrderResponse response = orderService.updateStatus(1L, request);

        assertThat(response).isSameAs(expected);
        verify(orderStatusUpdater, times(2)).update(1L, request);
    }

    @Test
    void updateStatus_exhaustsRetries_throwsInvalidOperation() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.SHIPPED);
        when(orderStatusUpdater.update(anyLong(), any()))
                .thenThrow(new ObjectOptimisticLockingFailureException("Order", null));

        assertThatThrownBy(() -> orderService.updateStatus(1L, request))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Could not update order status");
    }
}
