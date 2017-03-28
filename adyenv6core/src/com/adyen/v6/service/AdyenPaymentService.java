package com.adyen.v6.service;

import com.adyen.Client;
import com.adyen.Config;
import com.adyen.httpclient.HTTPClientException;
import com.adyen.model.PaymentRequest;
import com.adyen.model.PaymentRequest3d;
import com.adyen.model.PaymentResult;
import com.adyen.model.hpp.DirectoryLookupRequest;
import com.adyen.model.hpp.PaymentMethod;
import com.adyen.model.modification.CancelRequest;
import com.adyen.model.modification.CaptureRequest;
import com.adyen.model.modification.ModificationResult;
import com.adyen.model.modification.RefundRequest;
import com.adyen.model.recurring.RecurringDetail;
import com.adyen.model.recurring.RecurringDetailsRequest;
import com.adyen.model.recurring.RecurringDetailsResult;
import com.adyen.service.HostedPaymentPages;
import com.adyen.service.Modification;
import com.adyen.service.Payment;
import com.adyen.service.exception.ApiException;
import com.adyen.v6.factory.AdyenRequestFactory;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.core.model.user.CustomerModel;
import de.hybris.platform.payment.impl.DefaultPaymentServiceImpl;
import de.hybris.platform.store.BaseStoreModel;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SignatureException;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import static com.adyen.Client.HPP_LIVE;
import static com.adyen.Client.HPP_TEST;

//TODO: implement an interface
public class AdyenPaymentService extends DefaultPaymentServiceImpl {
    private BaseStoreModel baseStore;
    private AdyenRequestFactory adyenRequestFactory;

    private static final Logger LOG = Logger.getLogger(AdyenPaymentService.class);

    public Config getConfig() {
        Assert.notNull(baseStore);

        String username = baseStore.getAdyenUsername();
        String password = baseStore.getAdyenPassword();
        String merchantAccount = baseStore.getAdyenMerchantAccount();
        String skinCode = baseStore.getAdyenSkinCode();
        String hmacKey = baseStore.getAdyenSkinHMAC();
        String apiEndpoint = baseStore.getAdyenAPIEndpoint();
        boolean isHppTest = baseStore.getAdyenHppTest();

        Assert.notNull(merchantAccount);

        Config config = new Config();
        config.setUsername(username);
        config.setPassword(password);
        config.setMerchantAccount(merchantAccount);
        config.setSkinCode(skinCode);
        config.setHmacKey(hmacKey);
        config.setApplicationName("Hybris v6.0");
        config.setEndpoint(apiEndpoint);
        config.setHppEndpoint(HPP_LIVE);

        if (isHppTest) {
            config.setHppEndpoint(HPP_TEST);
        }

        return config;
    }

    /**
     * Returns a Client
     * It is not a singleton, so that it can support dynamic credential changes
     *
     * @return
     */
    private Client createClient() {
        Config config = getConfig();
        Client client = new Client(config);

        return client;
    }

    /**
     * Performs authorization request
     *
     * @param cartData
     * @param request
     * @param customerModel
     * @return
     * @throws Exception
     */
    public PaymentResult authorise(final CartData cartData, final HttpServletRequest request, final CustomerModel customerModel) throws Exception {
        Client client = createClient();
        Payment payment = new Payment(client);

        PaymentRequest paymentRequest = getAdyenRequestFactory().createAuthorizationRequest(
                client.getConfig().getMerchantAccount(),
                cartData,
                request,
                customerModel,
                baseStore.getAdyenRecurringContractMode()
        );

        return payment.authorise(paymentRequest);
    }

    /**
     * Performs 3D secure authorization request
     *
     * @param request
     * @param paRes
     * @param md
     * @return
     * @throws Exception
     */
    public PaymentResult authorise3D(final HttpServletRequest request,
                                     final String paRes,
                                     final String md) throws Exception {
        Client client = createClient();
        Payment payment = new Payment(client);

        PaymentRequest3d paymentRequest3d = getAdyenRequestFactory().create3DAuthorizationRequest(
                client.getConfig().getMerchantAccount(),
                request,
                md,
                paRes
        );

        LOG.info(paymentRequest3d); //TODO: anonymize
        PaymentResult paymentResult = payment.authorise3D(paymentRequest3d);
        LOG.info(paymentResult);

        return paymentResult;
    }

    /**
     * Performs Capture request
     *
     * @param amount
     * @param currency
     * @param authReference
     * @param merchantReference
     * @return
     * @throws Exception
     */
    public ModificationResult capture(final BigDecimal amount,
                                      final Currency currency,
                                      final String authReference,
                                      final String merchantReference) throws Exception {
        Client client = createClient();
        Modification modification = new Modification(client);

        CaptureRequest captureRequest = getAdyenRequestFactory().createCaptureRequest(
                client.getConfig().getMerchantAccount(),
                amount,
                currency,
                authReference,
                merchantReference
        );

        LOG.info(captureRequest);
        ModificationResult modificationResult = modification.capture(captureRequest);
        LOG.info(modificationResult);

        return modificationResult;
    }

    /**
     * Performs cancelOrRefund request
     *
     * @param authReference
     * @param merchantReference
     * @return
     * @throws Exception
     */
    public ModificationResult cancelOrRefund(final String authReference, final String merchantReference) throws Exception {
        Client client = createClient();
        Modification modification = new Modification(client);

        CancelRequest cancelRequest = getAdyenRequestFactory().createCancelRequest(
                client.getConfig().getMerchantAccount(),
                authReference,
                merchantReference
        );

        LOG.info(cancelRequest);
        ModificationResult modificationResult = modification.cancelOrRefund(cancelRequest);
        LOG.info(modificationResult);

        return modificationResult;
    }

    /**
     * Performs refund request
     *
     * @param amount
     * @param currency
     * @param authReference
     * @param merchantReference
     * @return
     * @throws Exception
     */
    public ModificationResult refund(final BigDecimal amount,
                                     final Currency currency,
                                     final String authReference,
                                     final String merchantReference) throws Exception {
        Client client = createClient();
        Modification modification = new Modification(client);

        RefundRequest refundRequest = getAdyenRequestFactory().createRefundRequest(
                client.getConfig().getMerchantAccount(),
                amount,
                currency,
                authReference,
                merchantReference
        );

        LOG.info(refundRequest);
        ModificationResult modificationResult = modification.refund(refundRequest);
        LOG.info(modificationResult);

        return modificationResult;
    }

    /**
     * Get Payment methods using HPP Directory Lookup
     *
     * @param amount
     * @param currency
     * @param countryCode
     * @return
     * @throws HTTPClientException
     * @throws SignatureException
     * @throws IOException
     */
    public List<PaymentMethod> getPaymentMethods(final BigDecimal amount,
                                                 final String currency,
                                                 final String countryCode) throws HTTPClientException, SignatureException, IOException {
        Client client = createClient();
        HostedPaymentPages hostedPaymentPages = new HostedPaymentPages(client);

        DirectoryLookupRequest directoryLookupRequest = getAdyenRequestFactory()
                .createListPaymentMethodsRequest(amount, currency, countryCode);

        LOG.info(directoryLookupRequest);
        List<PaymentMethod> paymentMethods = hostedPaymentPages.getPaymentMethods(directoryLookupRequest);
        LOG.info(paymentMethods);

        return paymentMethods;
    }

    public List<RecurringDetail> getStoredCards(final CustomerModel customerModel) throws IOException, ApiException {
        if (customerModel == null) {
            return null;
        }

        Client client = createClient();
        com.adyen.service.Recurring recurring = new com.adyen.service.Recurring(client);

        RecurringDetailsRequest request = getAdyenRequestFactory().createListRecurringDetailsRequest(
                client.getConfig().getMerchantAccount(),
                customerModel
        );

        LOG.info(request);
        RecurringDetailsResult result = recurring.listRecurringDetails(request);
        LOG.info(result);

        //Return only cards
        List<RecurringDetail> storedCards = result.getRecurringDetails().stream()
                .filter(detail -> (detail.getCard() != null && detail.getAlias() != null))
                .collect(Collectors.toList());

        return storedCards;
    }

    /**
     * Retrive the CSE JS Url
     *
     * @return
     */
    public String getCSEUrl() {
        Config config = getConfig();

        String cseId = baseStore.getAdyenCSEID();
        Assert.notNull(cseId);

        return config.getHppEndpoint() + "/cse/js/" + cseId + ".shtml";
    }

    public AdyenRequestFactory getAdyenRequestFactory() {
        if (adyenRequestFactory == null) {
            adyenRequestFactory = new AdyenRequestFactory();
        }

        return adyenRequestFactory;
    }

    public void setAdyenRequestFactory(AdyenRequestFactory adyenRequestFactory) {
        this.adyenRequestFactory = adyenRequestFactory;
    }

    public BaseStoreModel getBaseStore() {
        return baseStore;
    }

    public void setBaseStore(BaseStoreModel baseStore) {
        this.baseStore = baseStore;
    }
}
