# Wall War 🎮

Wall War is a strategic board game application for Android, featuring single-player (vs AI with Easy, Normal, and Pro difficulties) and local 2-player duel modes.

## CI/CD Telegram Delivery Workflow

This repository includes a GitHub Actions workflow (`.github/workflows/android-telegram-ci.yml`) that automatically builds the debug APK on every push to `main` / `master` and delivers the compiled APK directly to your Telegram chat or channel.

### Setting Up Telegram Bot Delivery on GitHub:

1. **Create a Telegram Bot:**
   - Message `@BotFather` on Telegram and create a new bot using `/newbot`.
   - Copy the generated `HTTP API Token` (e.g. `123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ`).

2. **Get Your Telegram Chat ID:**
   - Start a conversation with your bot or add it to your channel/group.
   - Message `@userinfobot` or `@raw_data_bot` to get your Telegram `chat_id`.

3. **Add Secrets to GitHub Repository:**
   - Go to your GitHub repository: `Settings` -> `Secrets and variables` -> `Actions`.
   - Click **New repository secret**.
   - Create secret `TELEGRAM_BOT_TOKEN` with your Bot Father token.
   - Create secret `TELEGRAM_CHAT_ID` with your chat/channel ID.

Whenever you push changes, GitHub Actions will compile the Android APK and send the document to your Telegram chat! 🚀
