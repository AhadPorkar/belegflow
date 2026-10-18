package com.belegflow.domain;
import java.math.BigDecimal;
import java.math.RoundingMode;
import static com.belegflow.domain.BusinessException.require;
/** This release accepts positive EUR invoices, not credit notes or FX accounting. */
public final class MoneyRules {
    private MoneyRules() {
    }
    public static BigDecimal amount(String value) {
        try {
            require(value != null && value.matches("-?[0-9]{1,12}(\\.[0-9]{1,2})?"), "INVALID_AMOUNT");
            BigDecimal amount = new BigDecimal(value);
            require(amount.scale() <= 2 && amount.precision() <= 14, "INVALID_AMOUNT");
            return amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (NumberFormatException | NullPointerException | ArithmeticException ex) {
            throw new BusinessException("INVALID_AMOUNT");
        }
    }
    public static boolean validTotals(BigDecimal net, BigDecimal tax, BigDecimal gross, String currency) {
        return "EUR".equals(currency) && net != null && tax != null && gross != null && net.signum() >= 0 && tax.signum() >= 0 && gross.signum() > 0 && net.scale() <= 2 && tax.scale() <= 2 && gross.scale() <= 2 && net.add(tax).compareTo(gross) == 0;
    }
    public static boolean needsSecondApproval(BigDecimal gross, BigDecimal threshold) {
        require(gross != null && threshold != null && threshold.signum() > 0, "INVALID_AMOUNT");
        return gross.compareTo(threshold) >= 0;
    }
}
