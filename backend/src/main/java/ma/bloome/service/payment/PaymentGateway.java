package ma.bloome.service.payment;

import ma.bloome.entity.CustomerOrder;
import ma.bloome.entity.Enums.PaymentMethod;
import ma.bloome.entity.Enums.PaymentStatus;

/**
 * Extension point for payment providers. A card provider (e.g. Stripe) would implement this
 * interface, create a payment intent in {@link #initiate} and confirm it from a webhook.
 * Orders must never be marked PAID unless a provider has actually confirmed the payment.
 */
public interface PaymentGateway {

    PaymentMethod method();

    /** Called once, inside the checkout transaction, after the order has been built. */
    PaymentResult initiate(CustomerOrder order);

    record PaymentResult(PaymentStatus status, String providerReference) {
    }
}
