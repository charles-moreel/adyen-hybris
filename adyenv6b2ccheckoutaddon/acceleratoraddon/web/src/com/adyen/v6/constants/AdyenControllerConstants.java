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
package com.adyen.v6.constants;

public interface AdyenControllerConstants
{
	String ADDON_PREFIX = "addon:/adyenv6b2ccheckoutaddon/";
	String CART_PREFIX = "/cart";
	String SELECT_PAYMENT_METHOD_PREFIX = "/checkout/multi/adyen/select-payment-method";
	String SUMMARY_CHECKOUT_PREFIX = "/checkout/multi/adyen/summary";
	String COMPONENT_PREFIX = "/adyen/component";

	/**
	 * Class with view name constants
	 */
	interface Views
	{

		interface Pages
		{

			interface MultiStepCheckout
			{
				String CheckoutSummaryPage = ADDON_PREFIX + "pages/checkout/multi/checkoutSummaryPage";
				String SelectPaymentMethod = ADDON_PREFIX + "pages/checkout/multi/selectPaymentMethodPage";
				String Validate3DSecurePaymentPage = ADDON_PREFIX + "pages/checkout/multi/3d-secure-payment-validation";
				String HppPaymentPage = ADDON_PREFIX + "pages/checkout/multi/hpp-payment";
				String Validate3DS2PaymentPage = ADDON_PREFIX + "pages/checkout/multi/3ds2_payment";
				String BillingAddressformPage = ADDON_PREFIX + "pages/checkout/multi/billingAddressForm";
			}
		}

	}
}
