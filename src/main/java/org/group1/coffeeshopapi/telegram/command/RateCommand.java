package org.group1.coffeeshopapi.telegram.command;

import lombok.RequiredArgsConstructor;
import org.group1.coffeeshopapi.bakong.BakongExchangeRateService;
import org.group1.coffeeshopapi.telegram.dto.TelegramMessage;
import org.group1.coffeeshopapi.telegram.util.TelegramFormat;
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
        return "💱 <b>Exchange Rate</b>\n\n1 USD = " + TelegramFormat.wholeAmount(rate, "KHR")
                + "\n\nUsed to convert your total when you pay by Bakong KHQR in KHR.";
    }

    @Override
    public boolean useHtml() {
        return true;
    }
}
