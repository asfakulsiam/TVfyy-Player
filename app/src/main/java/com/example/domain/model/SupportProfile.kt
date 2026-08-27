package com.example.domain.model

data class DeveloperProfile(
    val name: String = "ASFAKUL SIAM",
    val country: String = "BANGLADESH",
    val instagramUsername: String = "asfakulsiam",
    val instagramUrl: String = "https://instagram.com/asfakulsiam",
    val githubUsername: String = "asfakulsiam",
    val githubUrl: String = "https://github.com/asfakulsiam",
    val portfolioUrl: String = "https://asfakulsiam.dev.cv"
)

data class MobileBankingDetails(
    val providers: String = "Nagad / bKash / Rocket",
    val accountType: String = "Personal",
    val number: String = "01734737294"
)

data class BankTransferDetails(
    val bankName: String = "DUTCH-BANGLA BANK",
    val accountName: String = "ASFAKUL ISLAM",
    val accountNumber: String = "2067348739614",
    val branch: String = "Jamalpur Branch",
    val routingNumber: String = "090390854"
) {
    fun toFormattedDetails(): String {
        return """
BANK: $bankName
ACCOUNT NAME: $accountName
ACCOUNT NUMBER: $accountNumber
BRANCH: $branch
ROUTING NUMBER: $routingNumber
        """.trimIndent()
    }
}

data class CryptoWallet(
    val symbol: String,
    val name: String,
    val network: String,
    val address: String
)

data class OnlinePayment(
    val platform: String,
    val identifierLabel: String,
    val identifierValue: String
)

object SupportProfile {
    val developer = DeveloperProfile()
    val mobileBanking = MobileBankingDetails()
    val bankTransfer = BankTransferDetails()

    val cryptoWallets = listOf(
        CryptoWallet(
            symbol = "USDT",
            name = "Tether USD",
            network = "BNB Chain",
            address = "0x245125F8C5D3c814c4b1d1a3604160a39C21d0d2"
        ),
        CryptoWallet(
            symbol = "USDC",
            name = "USD Coin",
            network = "BNB Chain",
            address = "0x245125F8C5D3c814c4b1d1a3604160a39C21d0d2"
        ),
        CryptoWallet(
            symbol = "BTC",
            name = "Bitcoin",
            network = "Bitcoin",
            address = "bc1p36fvxlef8apl0c3vnu2hx286hfj57p2zqxzxxcksfgn3rq34zmasen754u"
        ),
        CryptoWallet(
            symbol = "TRX",
            name = "Tron",
            network = "Tron",
            address = "TY2MeH2NFxhNK5PiLAdfGXfaNiUvEg3shV"
        )
    )

    val onlinePayments = listOf(
        OnlinePayment(
            platform = "PayPal",
            identifierLabel = "Email",
            identifierValue = "asfakulsiam0@gmail.com"
        ),
        OnlinePayment(
            platform = "Binance Pay",
            identifierLabel = "UID",
            identifierValue = "54745610"
        ),
        OnlinePayment(
            platform = "Bybit",
            identifierLabel = "UID",
            identifierValue = "277953203"
        )
    )

    fun getAllPaymentDetailsText(): String {
        val sb = StringBuilder()
        sb.appendLine("TVfyy Player — Developer Support Details")
        sb.appendLine("Developer: ${developer.name} (${developer.country})")
        sb.appendLine()
        sb.appendLine("=== BANGLADESH ===")
        sb.appendLine("Mobile Banking (${mobileBanking.providers} - ${mobileBanking.accountType}): ${mobileBanking.number}")
        sb.appendLine()
        sb.appendLine("Bank Transfer:")
        sb.appendLine(bankTransfer.toFormattedDetails())
        sb.appendLine()
        sb.appendLine("=== INTERNATIONAL CRYPTO ===")
        cryptoWallets.forEach { wallet ->
            sb.appendLine("${wallet.symbol} (${wallet.network}): ${wallet.address}")
        }
        sb.appendLine()
        sb.appendLine("=== OTHER INTERNATIONAL ===")
        onlinePayments.forEach { payment ->
            sb.appendLine("${payment.platform} (${payment.identifierLabel}): ${payment.identifierValue}")
        }
        sb.appendLine()
        sb.append("Thank you for supporting TVfyy Player independent development!")
        return sb.toString()
    }
}
