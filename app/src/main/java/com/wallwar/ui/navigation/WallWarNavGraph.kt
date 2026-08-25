package com.wallwar.ui.navigation

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.wallwar.ui.AppScreen
import com.wallwar.ui.screens.auth.AuthScreen
import com.wallwar.ui.screens.auth.AuthViewModel
import com.wallwar.ui.screens.DailyQuestsScreen
import com.wallwar.ui.screens.DailyRewardsScreen
import com.wallwar.ui.screens.emoji.EmojiShopScreen
import com.wallwar.ui.screens.emoji.EmojiShopViewModel
import com.wallwar.ui.screens.skin.SkinShopScreen
import com.wallwar.ui.screens.skin.SkinShopViewModel
import com.wallwar.ui.screens.GameBoardScreen
import com.wallwar.ui.screens.HistoryScreen
import com.wallwar.ui.screens.HomeScreen
import com.wallwar.ui.screens.RulesScreen
import com.wallwar.ui.screens.SettingsScreen
import com.wallwar.ui.screens.shop.CoinShopScreen
import com.wallwar.ui.screens.shop.CoinShopViewModel
import com.wallwar.ui.screens.game.GameViewModel
import com.wallwar.ui.screens.history.HistoryViewModel
import com.wallwar.ui.screens.home.HomeViewModel
import com.wallwar.ui.screens.profile.ProfileScreen
import com.wallwar.ui.screens.profile.ProfileViewModel
import com.wallwar.ui.screens.ranking.RankingScreen
import com.wallwar.ui.screens.ranking.RankingViewModel
import com.wallwar.ui.screens.settings.SettingsViewModel

@Composable
fun WallWarNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier
    ) {
        composable<AuthRoute> {
            val viewModel: AuthViewModel = hiltViewModel()
            val authUiState by viewModel.authUiState.collectAsStateWithLifecycle()
            val isRegisterMode by viewModel.isRegisterMode.collectAsStateWithLifecycle()
            val hasSavedSession by viewModel.hasSavedSession.collectAsStateWithLifecycle()

            AuthScreen(
                authUiState = authUiState,
                isRegisterMode = isRegisterMode,
                hasSavedSession = hasSavedSession,
                onLoginEmail = viewModel::loginWithEmail,
                onRegisterEmail = viewModel::registerWithEmail,
                onSignInWithGoogle = viewModel::signInWithGoogle,
                onContinueAsGuest = viewModel::continueAsGuest,
                onPlayAsGuestDevice = viewModel::playAsGuestDevice,
                onToggleAuthMode = viewModel::toggleAuthMode,
                onClearError = viewModel::clearError,
                onAuthSuccess = {
                    navController.navigate(HomeRoute) {
                        popUpTo(AuthRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<HomeRoute> {
            val activity = LocalActivity.current
            val viewModel: HomeViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
            val totalMatches by viewModel.totalMatches.collectAsStateWithLifecycle()
            val arenaErrorMessage by viewModel.arenaErrorMessage.collectAsStateWithLifecycle()
            val bonusMessage by viewModel.bonusMessage.collectAsStateWithLifecycle()
            val abandonedMatchNotice by viewModel.abandonedMatchNotice.collectAsStateWithLifecycle()
            val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
            val isAdPlaying by viewModel.isAdPlaying.collectAsStateWithLifecycle()
            val isRewardedAdLoading by viewModel.isRewardedAdLoading.collectAsStateWithLifecycle()
            val dailyStreakState by viewModel.dailyStreakState.collectAsStateWithLifecycle()
            val dailyMissions by viewModel.dailyMissions.collectAsStateWithLifecycle()
            val spinnerState by viewModel.spinnerState.collectAsStateWithLifecycle()

            HomeScreen(
                userProfile = userProfile,
                totalWins = totalWins,
                totalMatches = totalMatches,
                onlineArenas = viewModel.onlineArenas,
                offlineArena = viewModel.offlineArena,
                boardTheme = boardTheme,
                arenaErrorMessage = arenaErrorMessage,
                bonusMessage = bonusMessage,
                abandonedMatchNotice = abandonedMatchNotice,
                isAdPlaying = isAdPlaying,
                isRewardedAdLoading = isRewardedAdLoading,
                dailyStreakState = dailyStreakState,
                dailyMissions = dailyMissions,
                spinnerState = spinnerState,
                onJoinOnlineArenaMatch = { arena ->
                    viewModel.joinOnlineArenaMatch(arena) { mode, opp, diff, ar ->
                        navController.navigate(
                            GameBoardRoute(
                                mode = mode.name,
                                opponent = opp.name,
                                difficulty = diff.name,
                                arenaId = ar.id
                            )
                        )
                    }
                },
                onJoinOfflineMatch = { opponentType, difficulty, useAd ->
                    if (useAd) {
                        viewModel.joinOfflineMatchWithAd(activity, opponentType, difficulty) { mode, opp, diff, ar ->
                            navController.navigate(
                                GameBoardRoute(
                                    mode = mode.name,
                                    opponent = opp.name,
                                    difficulty = diff.name,
                                    arenaId = ar.id
                                )
                            )
                        }
                    } else {
                        viewModel.joinOfflineMatch(opponentType, difficulty, false) { mode, opp, diff, ar ->
                            navController.navigate(
                                GameBoardRoute(
                                    mode = mode.name,
                                    opponent = opp.name,
                                    difficulty = diff.name,
                                    arenaId = ar.id
                                )
                            )
                        }
                    }
                },
                onClaimDailyBonus = viewModel::claimDailyBonus,
                onClaimMissionReward = viewModel::claimMissionReward,
                onSpinWheel = viewModel::spinWheel,
                onClearArenaError = viewModel::clearArenaErrorMessage,
                onClearBonusMessage = viewModel::clearBonusMessage,
                onClearAbandonedMatchNotice = viewModel::clearAbandonedMatchNotice,
                onNavigate = { targetScreen ->
                    when (targetScreen) {
                        AppScreen.GAME_BOARD -> navController.navigate(GameBoardRoute())
                        AppScreen.RULES -> navController.navigate(RulesRoute)
                        AppScreen.HISTORY -> navController.navigate(HistoryRoute)
                        AppScreen.SETTINGS -> navController.navigate(SettingsRoute)
                        AppScreen.COIN_SHOP -> navController.navigate(CoinShopRoute)
                        AppScreen.DAILY_REWARDS -> navController.navigate(DailyRewardsRoute)
                        AppScreen.DAILY_QUESTS -> navController.navigate(DailyQuestsRoute)
                        AppScreen.EMOJI_SHOP -> navController.navigate(SkinShopRoute(initialTab = 2))
                        AppScreen.SKIN_SHOP -> navController.navigate(SkinShopRoute(initialTab = 0))
                        AppScreen.PROFILE -> navController.navigate(ProfileRoute)
                        AppScreen.HOME -> navController.navigate(HomeRoute) {
                            popUpTo(HomeRoute) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable<RankingRoute> {
            val viewModel: RankingViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()

            RankingScreen(
                userProfile = userProfile,
                leaderboard = leaderboard,
                onNavigateToProfile = {
                    navController.navigate(ProfileRoute) {
                        popUpTo(HomeRoute)
                    }
                }
            )
        }

        composable<ProfileRoute> {
            val viewModel: ProfileViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val signInStatus by viewModel.signInStatus.collectAsStateWithLifecycle()
            val friends by viewModel.friends.collectAsStateWithLifecycle()

            ProfileScreen(
                userProfile = userProfile,
                signInStatus = signInStatus,
                friends = friends,
                onSignInWithGoogle = viewModel::signInWithGoogle,
                onClearSignInStatus = viewModel::clearSignInStatus,
                onSignOut = viewModel::signOut,
                onAddFriend = viewModel::addFriend,
                onRemoveFriend = viewModel::removeFriend,
                onChallengeFriend = { friendUsername ->
                    navController.navigate(GameBoardRoute(opponent = "ONLINE"))
                },
                onNavigateToHistory = { navController.navigate(HistoryRoute) },
                onNavigateToSettings = { navController.navigate(SettingsRoute) },
                onNavigateToCoinShop = { navController.navigate(CoinShopRoute) },
                onNavigateToAuth = {
                    navController.navigate(AuthRoute) {
                        popUpTo(HomeRoute) { inclusive = true }
                    }
                }
            )
        }

        composable<GameBoardRoute> {
            val viewModel: GameViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val gameState by viewModel.gameState.collectAsStateWithLifecycle()
            val boardTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
            val isWallMode by viewModel.isWallMode.collectAsStateWithLifecycle()
            val isWallHorizontal by viewModel.isWallHorizontal.collectAsStateWithLifecycle()
            val validHighlights by viewModel.validMoveHighlights.collectAsStateWithLifecycle()
            val onlineMatchState by viewModel.onlineMatchState.collectAsStateWithLifecycle()
            val onlineOpponentName by viewModel.onlineOpponentName.collectAsStateWithLifecycle()
            val myPlayerIndex by viewModel.myPlayerIndex.collectAsStateWithLifecycle()
            val turnTimeLeft by viewModel.turnTimeLeft.collectAsStateWithLifecycle()
            val isOpponentDisconnected by viewModel.isOpponentDisconnected.collectAsStateWithLifecycle()
            val disconnectSecondsRemaining by viewModel.disconnectSecondsRemaining.collectAsStateWithLifecycle()
            val isLocalDisconnected by viewModel.isLocalDisconnected.collectAsStateWithLifecycle()
            val localDisconnectSeconds by viewModel.localDisconnectSeconds.collectAsStateWithLifecycle()
            val onlineErrorMessage by viewModel.onlineErrorMessage.collectAsStateWithLifecycle()
            val matchResultDelta by viewModel.matchResultDelta.collectAsStateWithLifecycle()
            val playerEmote by viewModel.playerEmote.collectAsStateWithLifecycle()
            val opponentEmote by viewModel.opponentEmote.collectAsStateWithLifecycle()
            val unlockedEmojiIds by viewModel.unlockedEmojiIds.collectAsStateWithLifecycle()
            val equippedBallSkinId by viewModel.equippedBallSkinId.collectAsStateWithLifecycle()
            val equippedWallSkinId by viewModel.equippedWallSkinId.collectAsStateWithLifecycle()
            val opponentBallSkinId by viewModel.opponentBallSkinId.collectAsStateWithLifecycle()
            val opponentWallSkinId by viewModel.opponentWallSkinId.collectAsStateWithLifecycle()

            val activity = LocalActivity.current

            GameBoardScreen(
                gameState = gameState,
                boardTheme = boardTheme,
                isWallMode = isWallMode,
                isWallHorizontal = isWallHorizontal,
                validHighlights = validHighlights,
                soundManager = viewModel.soundManager,
                userDisplayName = userProfile.displayName,
                opponentType = viewModel.opponentType,
                onlineMatchState = onlineMatchState,
                onlineOpponentName = onlineOpponentName,
                myPlayerIndex = myPlayerIndex,
                turnTimeLeft = turnTimeLeft,
                isOpponentDisconnected = isOpponentDisconnected,
                disconnectSecondsRemaining = disconnectSecondsRemaining,
                isLocalDisconnected = isLocalDisconnected,
                localDisconnectSeconds = localDisconnectSeconds,
                arenaTitle = viewModel.selectedArena.title,
                winningPrize = viewModel.selectedArena.winningPrize,
                onlineErrorMessage = onlineErrorMessage,
                matchResultDelta = matchResultDelta,
                equippedBallSkinId = equippedBallSkinId,
                equippedWallSkinId = equippedWallSkinId,
                opponentBallSkinId = opponentBallSkinId,
                opponentWallSkinId = opponentWallSkinId,
                playerEmote = playerEmote,
                opponentEmote = opponentEmote,
                allEmojis = viewModel.allEmojis,
                unlockedEmojiIds = unlockedEmojiIds,
                onSendEmote = viewModel::sendEmote,
                onNavigateToEmojiShop = {
                    navController.navigate(SkinShopRoute(initialTab = 2))
                },
                onRetryOnlineConnection = viewModel::startOnlineMatchmaking,
                onCancelOnlineMatchmaking = viewModel::cancelOnlineMatchmaking,
                onForfeitAndQuitLocalMatch = viewModel::forfeitAndQuitLocalMatch,
                onCellClick = viewModel::selectCell,
                onPlaceWall = viewModel::placeWall,
                onSelectWallOrientation = viewModel::selectWallOrientation,
                onCancelWallMode = viewModel::toggleWallMode,
                onUndoMove = viewModel::undoMove,
                onRestart = viewModel::restartGame,
                onResign = viewModel::resignGame,
                onTriggerMatchEndInterstitial = { onClosed ->
                    viewModel.showMatchEndInterstitial(activity, onClosed)
                },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<RulesRoute> {
            RulesScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<HistoryRoute> {
            val viewModel: HistoryViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val onlineMatches by viewModel.onlineMatchHistory.collectAsStateWithLifecycle()
            val totalWins by viewModel.totalWins.collectAsStateWithLifecycle()
            val totalMatches by viewModel.totalMatches.collectAsStateWithLifecycle()

            HistoryScreen(
                userProfile = userProfile,
                matches = onlineMatches,
                totalWins = totalWins,
                totalMatches = totalMatches,
                onClearHistory = viewModel::clearHistory,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<SettingsRoute> {
            val viewModel: SettingsViewModel = hiltViewModel()
            val selectedTheme by viewModel.boardTheme.collectAsStateWithLifecycle()
            val nakamaConfig by viewModel.nakamaConfig.collectAsStateWithLifecycle()

            SettingsScreen(
                soundManager = viewModel.soundManager,
                selectedTheme = selectedTheme,
                nakamaConfig = nakamaConfig,
                onSelectTheme = viewModel::setBoardTheme,
                onUpdateNakamaConfig = viewModel::updateNakamaConfig,
                onTestConnection = viewModel::testNakamaConnection,
                onRestoreFromNakamaServer = viewModel::restoreFromNakamaServer,
                onExportDataBackup = viewModel::exportDataBackup,
                onRestoreDataFromBackup = viewModel::restoreDataFromBackup,
                onRestoreDefaultSettings = viewModel::restoreDefaultSettings,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<CoinShopRoute> {
            val viewModel: CoinShopViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val coinPacks by viewModel.coinPacks.collectAsStateWithLifecycle()
            val purchaseMessage by viewModel.purchaseMessage.collectAsStateWithLifecycle()
            val isPurchasing by viewModel.isPurchasing.collectAsStateWithLifecycle()
            val isRewardedAdLoading by viewModel.isRewardedAdLoading.collectAsStateWithLifecycle()
            val isRewardedAdReady by viewModel.isRewardedAdReady.collectAsStateWithLifecycle()
            val isAdPlaying by viewModel.isAdPlaying.collectAsStateWithLifecycle()

            val activity = LocalActivity.current

            CoinShopScreen(
                userProfile = userProfile,
                coinPacks = coinPacks,
                purchaseMessage = purchaseMessage,
                isPurchasing = isPurchasing,
                isRewardedAdLoading = isRewardedAdLoading,
                isRewardedAdReady = isRewardedAdReady,
                isAdPlaying = isAdPlaying,
                onWatchRewardedAd = { viewModel.watchRewardedAdForCoins(activity) },
                onBuyPack = { pack -> viewModel.buyCoinPack(activity, pack) },
                onClearMessage = viewModel::clearPurchaseMessage,
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<DailyRewardsRoute> {
            val viewModel: HomeViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val dailyStreakState by viewModel.dailyStreakState.collectAsStateWithLifecycle()

            DailyRewardsScreen(
                userProfile = userProfile,
                dailyStreakState = dailyStreakState,
                onClaimReward = viewModel::claimDailyBonus,
                onNavigateToShop = { navController.navigate(CoinShopRoute) },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<DailyQuestsRoute> {
            val viewModel: HomeViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val dailyMissions by viewModel.dailyMissions.collectAsStateWithLifecycle()

            DailyQuestsScreen(
                userProfile = userProfile,
                dailyMissions = dailyMissions,
                onClaimMissionReward = viewModel::claimMissionReward,
                onNavigateToShop = { navController.navigate(CoinShopRoute) },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<SkinShopRoute> {
            val viewModel: SkinShopViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val unlockedWallSkinIds by viewModel.unlockedWallSkinIds.collectAsStateWithLifecycle()
            val equippedWallSkinId by viewModel.equippedWallSkinId.collectAsStateWithLifecycle()
            val unlockedBallSkinIds by viewModel.unlockedBallSkinIds.collectAsStateWithLifecycle()
            val equippedBallSkinId by viewModel.equippedBallSkinId.collectAsStateWithLifecycle()
            val unlockedEmojiIds by viewModel.unlockedEmojiIds.collectAsStateWithLifecycle()
            val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
            val previewWallSkin by viewModel.previewWallSkin.collectAsStateWithLifecycle()
            val previewBallSkin by viewModel.previewBallSkin.collectAsStateWithLifecycle()
            val previewEmoji by viewModel.previewEmoji.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
            val insufficientCoinsInfo by viewModel.insufficientCoinsInfo.collectAsStateWithLifecycle()

            SkinShopScreen(
                userProfile = userProfile,
                allWallSkins = viewModel.allWallSkins,
                unlockedWallSkinIds = unlockedWallSkinIds,
                equippedWallSkinId = equippedWallSkinId,
                allBallSkins = viewModel.allBallSkins,
                unlockedBallSkinIds = unlockedBallSkinIds,
                equippedBallSkinId = equippedBallSkinId,
                allEmojis = viewModel.allEmojis,
                unlockedEmojiIds = unlockedEmojiIds,
                selectedTab = selectedTab,
                previewWallSkin = previewWallSkin,
                previewBallSkin = previewBallSkin,
                previewEmoji = previewEmoji,
                statusMessage = statusMessage,
                insufficientCoinsInfo = insufficientCoinsInfo,
                onDismissInsufficientCoinsDialog = viewModel::dismissInsufficientCoinsDialog,
                onSelectTab = viewModel::selectTab,
                onPreviewWallSkin = viewModel::previewWall,
                onClearWallPreview = viewModel::clearWallPreview,
                onBuyWallSkin = viewModel::buyWall,
                onEquipWallSkin = viewModel::equipWall,
                onPreviewBallSkin = viewModel::previewBall,
                onClearBallPreview = viewModel::clearBallPreview,
                onBuyBallSkin = viewModel::buyBall,
                onEquipBallSkin = viewModel::equipBall,
                onPreviewEmoji = viewModel::previewEmoji,
                onClearEmojiPreview = viewModel::clearEmojiPreview,
                onBuyEmoji = viewModel::buyEmoji,
                onClearStatusMessage = viewModel::clearStatusMessage,
                onOpenCoinShop = { navController.navigate(CoinShopRoute) },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }

        composable<EmojiShopRoute> {
            val viewModel: EmojiShopViewModel = hiltViewModel()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val unlockedEmojiIds by viewModel.unlockedEmojiIds.collectAsStateWithLifecycle()
            val previewEmoji by viewModel.previewEmoji.collectAsStateWithLifecycle()
            val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()

            EmojiShopScreen(
                userProfile = userProfile,
                allEmojis = viewModel.allEmojis,
                unlockedEmojiIds = unlockedEmojiIds,
                previewEmoji = previewEmoji,
                statusMessage = statusMessage,
                onPreviewEmoji = viewModel::preview,
                onBuyEmoji = { emoji -> viewModel.buyEmoji(emoji, onNavigateToCoinShop = { navController.navigate(CoinShopRoute) }) },
                onClearStatusMessage = viewModel::clearStatusMessage,
                onOpenCoinShop = { navController.navigate(CoinShopRoute) },
                onBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(HomeRoute)
                    }
                }
            )
        }
    }
}
