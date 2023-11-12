package org.telegram.toto.bots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Locality;
import org.telegram.abilitybots.api.objects.Privacy;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.toto.models.CalculateRequest;
import org.telegram.toto.models.CalculateResponse;
import org.telegram.toto.repository.ChatRepo;
import org.telegram.toto.service.CalculatePrizeService;
import org.telegram.toto.service.SubscriberService;
import org.telegram.toto.service.WebscrapperService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AhHuatBot extends AbilityBot {

    private ChatRepo telegramRepo;
    private WebscrapperService webscrapperService;
    private CalculatePrizeService calculatePrizeService;
    private SubscriberService subscriberService;
    private long creatorId;
    private static final Logger logger = LoggerFactory.getLogger(AhHuatBot.class);

    public AhHuatBot(
            String BOT_TOKEN,
            String BOT_USERNAME,
            ChatRepo telegramRepo,
            long creatorId,
            WebscrapperService webscrapperService,
            CalculatePrizeService calculatePrizeService,
            @Lazy SubscriberService subscriberService) {
        super(BOT_TOKEN, BOT_USERNAME);
        this.telegramRepo = telegramRepo;
        this.creatorId = creatorId;
        this.webscrapperService = webscrapperService;
        this.calculatePrizeService = calculatePrizeService;
        this.subscriberService = subscriberService;
    }

    public AhHuatBot(String BOT_TOKEN, String BOT_USERNAME) {
        super(BOT_TOKEN, BOT_USERNAME);
    }

    @Override
    public long creatorId() {
        return creatorId;
    }

    public Ability nextDrawCommand() {
        return Ability
                .builder()
                .name("next")
                .info("Get the upcoming draw prize money and datetime.")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    silent.send(
                            webscrapperService.getNextDrawInString(),
                            ctx.chatId());
                })
                .build();
    }

    public Ability subscribeCommand() {
        return Ability
                .builder()
                .name("subscribe")
                .info("To subscribe to alerts on the next draw")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {
                    subscriberService.saveChatPreferences(ctx);
                    boolean isNotified = subscriberService.initialNotifySubSilent(String.valueOf(ctx.chatId()));
                    if (!isNotified)
                        subscriberService.notifyFailSilent(Collections.singletonList(String.valueOf(ctx.chatId())));
                })
                .enableStats()
                .build();
    }

    public Ability unsubscribeCommand() {
        return Ability
                .builder()
                .name("unsubscribe")
                .info("To unsubscribe from the alerts.")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> telegramRepo.deleteById(String.valueOf(ctx.chatId())))
                .post(ctx -> silent.send("You have unsubscribed", ctx.chatId()))
                .build();
    }

    public Ability prevCommand() {
        return Ability
                .builder()
                .name("prev")
                .info("Get previous draw")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {

                    String prevDraw = webscrapperService.getPreviousDraw();

                    SendMessage sendMessage = new SendMessage();

                    sendMessage.enableMarkdown(true);
                    sendMessage.setProtectContent(true);
                    sendMessage.setText(prevDraw);
                    sendMessage.setChatId(ctx.chatId());

                    silent.execute(sendMessage);

                })
                .build();
    }

    public Ability calculateWinningsCommand() {
        return Ability
                .builder()
                .name("calculate")
                .info("calculate winnings based on numbers passed")
                .locality(Locality.ALL)
                .privacy(Privacy.PUBLIC)
                .action(ctx -> {

                    Optional<Integer> opt = webscrapperService.getLastDrawNo();
                    if (opt.isEmpty()) {
                        silent.send("Unable to calculate", ctx.chatId());
                    } else {

                        CalculateRequest calculateRequest = new CalculateRequest(
                                opt.get(),
                                false,
                                ctx.firstArg(),
                                1,
                                1);

                        CalculateResponse calculateResponse = calculatePrizeService
                                .getCalculateResults(calculateRequest);

                        int totalValueWon = calculateResponse.getD().getPrizes().stream()
                                .map(prize -> prize.getTotal())
                                .reduce(
                                        0,
                                        (subtotal, element) -> subtotal + element);

                        if (totalValueWon == 0) {
                            silent.send("You did not win any prize", ctx.chatId());
                        } else {

                            StringBuilder sb = new StringBuilder();

                            List<String> submittedNumbers = Arrays.asList(ctx.firstArg().split(","));
                            Collections.sort(submittedNumbers);

                            sb.append("You have won $").append(totalValueWon).append("\n");
                            sb.append("```\n");
                            sb.append("Your numbers are: ").append(submittedNumbers
                                    .toString()
                                    .replace("[", " ")
                                    .replace("]", " ")).append("\n");

                            sb.append("Winning numbers are: ").append(calculateResponse
                                    .getD()
                                    .getWinningNumbers()
                                    .toString()
                                    .replace("[", "")
                                    .replace("]", "")).append("\n");

                            sb.append("\n");
                            sb.append(String.format("| %-5s | %-12s | %-14s |%n", "Group", "Amount", "No. of shares"));
                            String s1 = "-".repeat(5);
                            String s2 = "-".repeat(12);
                            String s3 = "-".repeat(14);
                            sb.append(String.format("| %s | %s | %s |%n", s1, s2, s3));

                            calculateResponse.getD().getPrizes().forEach(prize -> {

                                sb.append(String.format(
                                        "| %-5s | $%-11s | %-14s |%n",
                                        prize.getGroupNumber(),
                                        prize.getShareAmount(),
                                        prize.getNumberOfSharesWon()));
                            });
                            sb.append("\n");
                            sb.append("```");

                            SendMessage sendMessage = new SendMessage();

                            sendMessage.enableMarkdown(true);
                            sendMessage.setProtectContent(true);
                            sendMessage.setText(sb.toString());
                            sendMessage.setChatId(ctx.chatId());

                            try {
                                sender.execute(sendMessage);
                            } catch (TelegramApiException e) {
                                logger.error(e.getMessage(), e);
                            }

                        }

                    }

                })
                .build();
    }

}
