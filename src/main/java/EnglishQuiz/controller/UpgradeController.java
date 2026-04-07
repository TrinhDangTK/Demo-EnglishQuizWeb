package EnglishQuiz.controller;

import EnglishQuiz.model.UserAccount;
import EnglishQuiz.repository.UserAccountRepository;
import EnglishQuiz.service.MomoService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Controller
public class UpgradeController {

    private static final Logger log = LoggerFactory.getLogger(UpgradeController.class);

    /** VIP upgrade price in VND */
    private static final long VIP_PRICE = 199000;

    private final UserAccountRepository userAccountRepository;
    private final MomoService momoService;

    public UpgradeController(UserAccountRepository userAccountRepository,
                             MomoService momoService) {
        this.userAccountRepository = userAccountRepository;
        this.momoService = momoService;
    }

    // ── Upgrade page ──────────────────────────────────────────

    @GetMapping("/upgrade")
    public String upgradePage(HttpSession session, Model model) {
        Object nameObj = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(nameObj instanceof String username) || username.isBlank()) {
            return "redirect:/login?redirect=" + URLEncoder.encode("/upgrade", StandardCharsets.UTF_8);
        }

        Object tierObj = session.getAttribute(AuthController.SESSION_TIER_KEY);
        int tier = (tierObj instanceof Integer t) ? t : AuthController.TIER_NORMAL;
        model.addAttribute("currentTier", tier);
        model.addAttribute("alreadyVip", tier >= AuthController.TIER_VIP);
        model.addAttribute("vipPrice", VIP_PRICE);
        return "upgrade";
    }

    // ── Create MoMo payment ───────────────────────────────────

    @PostMapping("/upgrade")
    public String createMomoPayment(HttpSession session, RedirectAttributes redirectAttributes) {
        Object nameObj = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (!(nameObj instanceof String username) || username.isBlank()) {
            return "redirect:/login?redirect=" + URLEncoder.encode("/upgrade", StandardCharsets.UTF_8);
        }

        // Check if already VIP
        Object tierObj = session.getAttribute(AuthController.SESSION_TIER_KEY);
        int tier = (tierObj instanceof Integer t) ? t : AuthController.TIER_NORMAL;
        if (tier >= AuthController.TIER_VIP) {
            redirectAttributes.addFlashAttribute("success", "You are already a VIP member! 🎉");
            return "redirect:/upgrade";
        }

        try {
            // Encode username in extraData so we can identify the user on callback
            String extraData = Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8));
            String orderInfo = "EnglishQuiz VIP Upgrade - " + username;

            String payUrl = momoService.createPaymentUrl(VIP_PRICE, orderInfo, extraData);

            if (payUrl != null && !payUrl.isBlank()) {
                return "redirect:" + payUrl;
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Could not create MoMo payment. Please try again later.");
                return "redirect:/upgrade";
            }
        } catch (Exception e) {
            log.error("MoMo payment creation failed", e);
            redirectAttributes.addFlashAttribute("error",
                    "❌ Payment service error. Please try again later.");
            return "redirect:/upgrade";
        }
    }

    // ── MoMo callback (redirect after payment) ────────────────

    @GetMapping("/payment/momo-return")
    public String momoReturn(
            @RequestParam(required = false) String partnerCode,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Long amount,
            @RequestParam(required = false) String orderInfo,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) Long transId,
            @RequestParam(required = false) Integer resultCode,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String payType,
            @RequestParam(required = false) Long responseTime,
            @RequestParam(required = false) String extraData,
            @RequestParam(required = false) String signature,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        log.info("MoMo return: orderId={}, resultCode={}, message={}, transId={}",
                orderId, resultCode, message, transId);

        // Verify signature
        String rawSignature = "accessKey=" + momoService.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + (extraData != null ? extraData : "")
                + "&message=" + (message != null ? message : "")
                + "&orderId=" + orderId
                + "&orderInfo=" + (orderInfo != null ? orderInfo : "")
                + "&orderType=" + (orderType != null ? orderType : "")
                + "&partnerCode=" + (partnerCode != null ? partnerCode : "")
                + "&payType=" + (payType != null ? payType : "")
                + "&requestId=" + requestId
                + "&responseTime=" + (responseTime != null ? responseTime : "")
                + "&resultCode=" + resultCode
                + "&transId=" + transId;

        if (signature != null && !momoService.verifySignature(rawSignature, signature)) {
            log.warn("MoMo return signature mismatch!");
            redirectAttributes.addFlashAttribute("error", "❌ Payment verification failed. Signature mismatch.");
            return "redirect:/upgrade";
        }

        if (resultCode != null && resultCode == 0) {
            // Payment successful — upgrade user to VIP
            String username = decodeUsername(extraData, session);
            if (username != null) {
                upgradeUserToVip(username, session);
                redirectAttributes.addFlashAttribute("success",
                        "🎉 Payment successful! You are now a VIP member. Transaction ID: " + transId);
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "❌ Payment was successful but we could not identify your account. Please contact support.");
            }
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "❌ Payment was not completed. " + (message != null ? message : "Please try again."));
        }

        return "redirect:/upgrade";
    }

    // ── MoMo IPN (server-to-server notification) ──────────────

    @PostMapping("/payment/momo-ipn")
    @org.springframework.web.bind.annotation.ResponseBody
    public String momoIpn(
            @RequestParam(required = false) String partnerCode,
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Long amount,
            @RequestParam(required = false) String orderInfo,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false) Long transId,
            @RequestParam(required = false) Integer resultCode,
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String payType,
            @RequestParam(required = false) Long responseTime,
            @RequestParam(required = false) String extraData,
            @RequestParam(required = false) String signature) {

        log.info("MoMo IPN: orderId={}, resultCode={}, transId={}", orderId, resultCode, transId);

        if (resultCode != null && resultCode == 0) {
            String username = decodeUsername(extraData, null);
            if (username != null) {
                upgradeUserToVip(username, null);
                log.info("IPN: Upgraded user '{}' to VIP", username);
            }
        }

        // MoMo expects a 204 No Content or a JSON response
        return "{\"status\":\"ok\"}";
    }

    // ── Helpers ────────────────────────────────────────────────

    private String decodeUsername(String extraData, HttpSession session) {
        // Try to decode from extraData (Base64-encoded username)
        if (extraData != null && !extraData.isBlank()) {
            try {
                return new String(Base64.getDecoder().decode(extraData), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("Could not decode extraData: {}", extraData);
            }
        }
        // Fallback to session
        if (session != null) {
            Object nameObj = session.getAttribute(AuthController.SESSION_USER_KEY);
            if (nameObj instanceof String username && !username.isBlank()) {
                return username;
            }
        }
        return null;
    }

    private void upgradeUserToVip(String username, HttpSession session) {
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
        if (user == null) {
            log.error("Cannot find user '{}' to upgrade", username);
            return;
        }
        if (user.getTier() != null && user.getTier() >= AuthController.TIER_VIP) {
            log.info("User '{}' is already VIP", username);
            return;
        }

        user.setTier(AuthController.TIER_VIP);
        userAccountRepository.save(user);
        log.info("User '{}' upgraded to VIP", username);

        // Update session if available
        if (session != null) {
            session.setAttribute(AuthController.SESSION_TIER_KEY, AuthController.TIER_VIP);
        }
    }
}
