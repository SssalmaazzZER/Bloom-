package ma.bloome.service.payment;

import ma.bloome.entity.CustomerOrder;
import ma.bloome.entity.Enums.PaymentMethod;
import ma.bloome.entity.Enums.PaymentStatus;
import org.springframework.stereotype.Component;

/**
 * Cash on delivery: nothing is charged at checkout. The order stays UNPAID until the courier
 * collects the cash, which is recorded when the order is marked DELIVERED.
 */
@Component
public class CashOnDeliveryGateway implements PaymentGateway {

    @Override
    public PaymentMethod method() {
        return PaymentMethod.CASH_ON_DELIVERY;
    }

    @Override
    public PaymentResult initiate(CustomerOrder order) {
        return new PaymentResult(PaymentStatus.UNPAID, null);
    }
}
