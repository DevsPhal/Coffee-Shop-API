package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class RateCommand implements TelegramCommand {
    private final BakongExchangeRateService bakongExchangeRateService;

    @Override
    public String name() {
        return "/rate";
    }

    @Override
    public String execute(TelegramMessage message, String argument) {
        BigDecimal rate = bakongExchangeRateService.getCurrentRate();
        return "💱 <b>Exchange Rate</b>\n\n1 USD = " + rate.stripTrailingZeros().toPlainString() + " KHR"
                + "\n\nUsed to convert your total when you pay in KHR. Riel totals are rounded to the nearest"
                + " 100 KHR: 00–50 rounds down, 51–99 rounds up (1,250 → 1,200; 1,251 → 1,300).";
    }

    @Override
    public boolean useHtml() {
        return true;
    }
}
