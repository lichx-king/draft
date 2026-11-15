package com.aims.services;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/**
 * Phí giao hàng (không chịu VAT):
 *  - Hà Nội / TP.HCM: 22.500đ cho 3kg đầu. Tỉnh khác: 40.000đ cho 1,5kg đầu.
 *  - Mỗi 0,5kg tiếp theo (làm tròn lên): +3.500đ.
 *  - Tiền hàng (chưa VAT) > 200.000đ: giảm phí giao hàng tối đa 25.000đ.
 */
@Component
public class ShippingFeeCalculator {

    public static final long FREE_SHIP_THRESHOLD = 200_000;
    public static final long FREE_SHIP_MAX_DISCOUNT = 25_000;

    public long calculate(String province, double totalWeightKg, long itemsTotalExcludingVat) {
        boolean bigCity = isBigCity(province);
        double firstKg = bigCity ? 3.0 : 1.5;
        long fee = bigCity ? 22_500 : 40_000;

        if (totalWeightKg > firstKg) {
            long steps = (long) Math.ceil((totalWeightKg - firstKg) / 0.5 - 1e-9);
            fee += steps * 3_500;
        }
        return fee - freeShippingDiscount(itemsTotalExcludingVat, fee);
    }

    public long freeShippingDiscount(long itemsTotalExcludingVat, long fee) {
        if (itemsTotalExcludingVat > FREE_SHIP_THRESHOLD) {
            return Math.min(fee, FREE_SHIP_MAX_DISCOUNT);
        }
        return 0;
    }

    private boolean isBigCity(String province) {
        if (province == null) {
            return false;
        }
        String s = Normalizer.normalize(province, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('đ', 'd').replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
        return s.contains("ha noi") || s.contains("ho chi minh") || s.contains("hcm");
    }
}
