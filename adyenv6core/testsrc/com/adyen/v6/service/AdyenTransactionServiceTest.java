/*
 *                        ######
 *                        ######
 *  ############    ####( ######  #####. ######  ############   ############
 *  #############  #####( ######  #####. ######  #############  #############
 *         ######  #####( ######  #####. ######  #####  ######  #####  ######
 *  ###### ######  #####( ######  #####. ######  #####  #####   #####  ######
 *  ###### ######  #####( ######  #####. ######  #####          #####  ######
 *  #############  #############  #############  #############  #####  ######
 *   ############   ############  #############   ############  #####  ######
 *                                       ######
 *                                #############
 *                                ############
 *
 *  Adyen Hybris Extension
 *
 *  Copyright (c) 2017 Adyen B.V.
 *  This file is open source and available under the MIT license.
 *  See the LICENSE file for more info.
 */
package com.adyen.v6.service;

import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.v6.model.NotificationItemModel;
import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.core.model.c2l.CurrencyModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.order.payment.PaymentInfoModel;
import de.hybris.platform.orderprocessing.model.OrderProcessModel;
import de.hybris.platform.payment.dto.TransactionStatus;
import de.hybris.platform.payment.model.PaymentTransactionEntryModel;
import de.hybris.platform.payment.model.PaymentTransactionModel;
import de.hybris.platform.servicelayer.i18n.CommonI18NService;
import de.hybris.platform.servicelayer.model.ModelService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collection;

import static com.adyen.model.notification.NotificationRequestItem.EVENT_CODE_AUTHORISATION;
import static com.adyen.model.notification.NotificationRequestItem.EVENT_CODE_CAPTURE;
import static de.hybris.platform.payment.dto.TransactionStatus.ACCEPTED;
import static de.hybris.platform.payment.dto.TransactionStatus.REJECTED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class AdyenTransactionServiceTest {

    private static final String MERCHANT_REFERENCE = "merchantReference";
    private static final String PSP_REFERENCE = "pspReference";

    @Mock
    private ModelService modelServiceMock;

    @Mock
    private CommonI18NService commonI18NServiceMock;

    private DefaultAdyenTransactionService adyenTransactionService;

    @Before
    public void setUp() {
        when(modelServiceMock.create(PaymentTransactionEntryModel.class))
                .thenReturn(new PaymentTransactionEntryModel());

        PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
        paymentTransactionModel.setEntries(new ArrayList<>());
        when(modelServiceMock.create(PaymentTransactionModel.class))
                .thenReturn(paymentTransactionModel);

        adyenTransactionService = new DefaultAdyenTransactionService();

        adyenTransactionService.setModelService(modelServiceMock);
        adyenTransactionService.setCommonI18NService(commonI18NServiceMock);
    }

    @Test
    public void testCreateCapturedTransactionFromNotification() {
        NotificationItemModel notificationItemModel = new NotificationItemModel();
        notificationItemModel.setPspReference(PSP_REFERENCE);
        notificationItemModel.setEventCode(EVENT_CODE_CAPTURE);
        notificationItemModel.setSuccess(true);

        PaymentTransactionModel paymentTransactionModel = new PaymentTransactionModel();
        paymentTransactionModel.setEntries(new ArrayList<>());

        PaymentTransactionEntryModel paymentTransactionEntryModel = adyenTransactionService
                .createCapturedTransactionFromNotification(paymentTransactionModel, notificationItemModel);

        assertEquals(PSP_REFERENCE, paymentTransactionEntryModel.getRequestId());
        assertEquals(ACCEPTED.name(), paymentTransactionEntryModel.getTransactionStatus());

        //Test non-successful notification
        notificationItemModel.setSuccess(false);
        paymentTransactionEntryModel = adyenTransactionService
                .createCapturedTransactionFromNotification(paymentTransactionModel, notificationItemModel);

        assertEquals(REJECTED.name(), paymentTransactionEntryModel.getTransactionStatus());
    }

    /**
     * Test authorizeOrderModel
     *
     * @throws Exception
     */
    @Test
    public void testAuthorizeOrderModel() {
        OrderModel orderModel = createDummyOrderModel();

        PaymentTransactionModel paymentTransactionModel = adyenTransactionService
                .authorizeOrderModel(orderModel, MERCHANT_REFERENCE, PSP_REFERENCE);

        //Verify that the payment transaction is saved
        verify(modelServiceMock).save(paymentTransactionModel);
    }

    @Test
    public void testStoreFailedAuthorizationFromNotification() {
        NotificationItemModel notificationItemModel = new NotificationItemModel();
        notificationItemModel.setPspReference(PSP_REFERENCE);
        notificationItemModel.setMerchantReference(MERCHANT_REFERENCE);
        notificationItemModel.setEventCode(EVENT_CODE_AUTHORISATION);
        notificationItemModel.setSuccess(false);

        OrderModel orderModel = createDummyOrderModel();

        PaymentTransactionModel paymentTransactionModel = adyenTransactionService
                .storeFailedAuthorizationFromNotification(notificationItemModel, orderModel);

        //Verify that the payment transaction is saved
        verify(modelServiceMock).save(paymentTransactionModel);
    }

    @Test
    public void testCreatePaymentTransactionFromAuthorisedResultCode() {
        OrderModel orderModel = createDummyOrderModel();

        PaymentTransactionModel paymentTransactionModel = adyenTransactionService
                .createPaymentTransactionFromResultCode(orderModel, MERCHANT_REFERENCE, PSP_REFERENCE, PaymentsResponse.ResultCodeEnum.AUTHORISED);

        //Verify that the payment transaction is saved
        verify(modelServiceMock).save(paymentTransactionModel);
        assertNotNull(paymentTransactionModel.getEntries());
        assertNotNull(paymentTransactionModel.getEntries().get(0));
        assertEquals(ACCEPTED.name(), paymentTransactionModel.getEntries().get(0).getTransactionStatus());
    }

    @Test
    public void testCreatePaymentTransactionFromRefusedResultCode() {
        OrderModel orderModel = createDummyOrderModel();

        PaymentTransactionModel paymentTransactionModel = adyenTransactionService
                .createPaymentTransactionFromResultCode(orderModel, MERCHANT_REFERENCE, PSP_REFERENCE, PaymentsResponse.ResultCodeEnum.REFUSED);

        //Verify that the payment transaction is saved
        verify(modelServiceMock).save(paymentTransactionModel);
        assertNotNull(paymentTransactionModel.getEntries());
        assertNotNull(paymentTransactionModel.getEntries().get(0));
        assertEquals(REJECTED.name(), paymentTransactionModel.getEntries().get(0).getTransactionStatus());
    }

    private OrderModel createDummyOrderModel() {
        Collection<OrderProcessModel> orderProcessModels = new ArrayList<OrderProcessModel>();
        OrderProcessModel orderProcessModel = new OrderProcessModel();
        orderProcessModel.setCode("order_process_code");
        orderProcessModels.add(orderProcessModel);

        OrderModel orderModel = new OrderModel();
        orderModel.setOrderProcess(orderProcessModels);
        orderModel.setTotalPrice(1.23);
        orderModel.setCurrency(new CurrencyModel());

        PaymentInfoModel paymentInfoModel = new PaymentInfoModel();
        paymentInfoModel.setAdyenPaymentMethod("visa");

        orderModel.setPaymentInfo(paymentInfoModel);

        return orderModel;
    }
}
