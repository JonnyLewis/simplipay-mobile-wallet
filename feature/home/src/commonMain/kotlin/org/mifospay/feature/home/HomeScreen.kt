/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mobile_wallet.feature.home.generated.resources.Res
import mobile_wallet.feature.home.generated.resources.arrow_backward
import mobile_wallet.feature.home.generated.resources.feature_home_account_number
import mobile_wallet.feature.home.generated.resources.feature_home_mark_default
import mobile_wallet.feature.home.generated.resources.feature_home_no_account
import mobile_wallet.feature.home.generated.resources.feature_home_view_more
import mobile_wallet.feature.home.generated.resources.feature_home_wallet_balance
import mobile_wallet.feature.home.generated.resources.home_no_transactions_found
import mobile_wallet.feature.home.generated.resources.home_transaction_history
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.mifospay.core.common.MoneyFormat
import org.mifospay.core.common.WalletNaming
import org.mifospay.core.designsystem.component.AnimatedWalletWordmark
import org.mifospay.core.designsystem.component.BasicDialogState
import org.mifospay.core.designsystem.component.LoadingDialogState
import org.mifospay.core.designsystem.component.MifosBasicDialog
import org.mifospay.core.designsystem.component.MifosLoadingDialog
import org.mifospay.core.designsystem.component.MifosScaffold
import org.mifospay.core.designsystem.component.Rosette
import org.mifospay.core.designsystem.component.scrollbar.DraggableScrollbar
import org.mifospay.core.designsystem.component.scrollbar.rememberDraggableScroller
import org.mifospay.core.designsystem.component.scrollbar.scrollbarState
import org.mifospay.core.designsystem.icon.MifosIcons
import org.mifospay.core.designsystem.theme.MifosTheme
import org.mifospay.core.designsystem.theme.SimpliPayTheme
import org.mifospay.core.model.account.Account
import org.mifospay.core.model.savingsaccount.Currency
import org.mifospay.core.model.savingsaccount.Status
import org.mifospay.core.model.savingsaccount.Transaction
import org.mifospay.core.model.savingsaccount.TransactionType
import org.mifospay.core.ui.EmptyContentScreen
import org.mifospay.core.ui.ErrorScreenContent
import org.mifospay.core.ui.MifosDivider
import org.mifospay.core.ui.MifosProgressIndicator
import org.mifospay.core.ui.MifosProgressIndicatorMini
import org.mifospay.core.ui.MifosSmallChip
import org.mifospay.core.ui.TransactionFilterBottomSheet
import org.mifospay.core.ui.TransactionItem
import org.mifospay.core.ui.utils.EventsEffect
import template.core.base.designsystem.theme.KptTheme

/*
 * Feature Enhancement
 * Show all saving accounts as stacked card
 * Show transaction history of selected account
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    onNavigateBack: () -> Unit,
    onRequest: (String) -> Unit,
    onPay: () -> Unit,
    onTopUp: () -> Unit,
    onBuy: () -> Unit,
    navigateToTransactionDetail: (Long, Long) -> Unit,
    navigateToAccountDetail: (Long) -> Unit,
    navigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) {
        viewModel.getAccounts()
    }

    val snackbarState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val homeUIState by viewModel.stateFlow.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    EventsEffect(viewModel) { event ->
        when (event) {
            is HomeEvent.NavigateBack -> onNavigateBack()
            is HomeEvent.NavigateToRequestScreen -> onRequest(event.vpa)
            is HomeEvent.NavigateToSendScreen -> onPay()
            is HomeEvent.NavigateToTopUpScreen -> onTopUp.invoke()
            is HomeEvent.NavigateToBuyScreen -> onBuy()
            is HomeEvent.NavigateToClientDetailScreen -> {}
            is HomeEvent.NavigateToTransactionDetail -> {
                navigateToTransactionDetail(event.accountId, event.transactionId)
            }

            is HomeEvent.NavigateToTransactionScreen -> navigateToHistory()
            is HomeEvent.ShowToast -> {
                scope.launch {
                    snackbarState.showSnackbar(getString(event.message))
                }
            }

            is HomeEvent.NavigateToAccountDetail -> {
                navigateToAccountDetail(event.accountId)
            }
        }
    }

    HomeScreenDialog(
        dialogState = homeUIState.dialogState,
        onDismissRequest = remember(viewModel) {
            { viewModel.trySendAction(HomeAction.OnDismissDialog) }
        },
    )

    HomeScreenContent(
        viewState = homeUIState.viewState,
        defaultAccountId = homeUIState.defaultAccountId,
        snackbarHostState = snackbarState,
        isRefreshing = homeUIState.isRefreshing,
        pullRefreshState = pullRefreshState,
        modifier = modifier,
        onAction = remember(viewModel) {
            { viewModel.trySendAction(it) }
        },
        uiState = homeUIState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    uiState: HomeState,
    viewState: ViewState,
    defaultAccountId: Long?,
    snackbarHostState: SnackbarHostState,
    isRefreshing: Boolean = false,
    pullRefreshState: PullToRefreshState,
    modifier: Modifier = Modifier,
    onAction: (HomeAction) -> Unit,
) {
    MifosScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { onAction(HomeAction.OnPullToRefresh) },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(it),
            contentAlignment = Alignment.Center,
        ) {
            when (viewState) {
                is ViewState.Loading -> MifosProgressIndicator()

                is ViewState.Content -> {
                    HomeScreenContent(
                        transactions = uiState.transactions,
                        accounts = uiState.accounts,
                        defaultAccountId = defaultAccountId,
                        onAction = onAction,
                        modifier = Modifier,
                        showBottomSheet = uiState.showBottomSheet,
                        transactionType = uiState.transactionType,
                        selectedTransactionType = uiState.currentSelectedTransactionType,
                        currentSelectedAccount = uiState.currentSelectedAccount,
                        selectedAccount = uiState.selectedAccount,
                        transactionLoading = uiState.transactionsLoading,
                    )
                }

                is ViewState.Error -> {
                    ErrorScreenContent(
                        subTitle = viewState.message,
                        onClickRetry = {
                            onAction(HomeAction.OnRetryClicked)
                        },
                    )
                }

                ViewState.NoAccounts -> {
                    EmptyContentScreen(
                        title = stringResource(Res.string.feature_home_no_account),
                        subTitle = "",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun HomeScreenContent(
    transactionLoading: Boolean,
    showBottomSheet: Boolean,
    currentSelectedAccount: Account?,
    selectedAccount: Account?,
    accounts: List<Account>,
    transactions: List<Transaction>?,
    defaultAccountId: Long?,
    selectedTransactionType: TransactionType,
    transactionType: TransactionType,
    onAction: (HomeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = calculateWindowSizeClass()
    val state = rememberLazyListState()

    val scrollbarState = state.scrollbarState(itemsAvailable = state.layoutInfo.totalItemsCount)
    val showScrollBar = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            state = state,
            contentPadding = PaddingValues(),
        ) {
            item {
                AnimatedWalletWordmark(
                    fontSize = 24.sp,
                    modifier = Modifier.padding(
                        start = KptTheme.spacing.md,
                        top = KptTheme.spacing.md,
                        bottom = KptTheme.spacing.sm,
                    ),
                )
            }

            item {
                AccountList(
                    accounts = accounts,
                    defaultAccountId = defaultAccountId,
                    onClick = {
                        onAction(HomeAction.AccountDetailsClicked(it))
                    },
                    onMarkAsDefault = { accId, accNo ->
                        onAction(HomeAction.MarkAsDefault(accId, accNo))
                    },
                    onPageChanged = {
                        onAction(HomeAction.OnSelectedAccountChanged(accounts[it]))
                    },
                    onTopUp = {
                        onAction(HomeAction.TopUpClicked)
                    },
                )
            }

            item {
                PayRequestScreen(
                    modifier = Modifier.padding(
                        vertical = KptTheme.spacing.md,
                        horizontal = KptTheme.spacing.md,
                    ),
                    onRequest = {
                        onAction(HomeAction.RequestClicked)
                    },
                    onSend = {
                        onAction(HomeAction.SendClicked)
                    },
                    onBuy = {
                        onAction(HomeAction.BuyClicked)
                    },
                    onTopUp = {
                        onAction(HomeAction.TopUpClicked)
                    },
                )
            }

            item {
                HomeTransactionHistoryCard(
                    modifier = Modifier.padding(
                        vertical = KptTheme.spacing.md,
                        horizontal = KptTheme.spacing.md,
                    ),
                    transactions = transactions,
                    selectedAccount = selectedAccount?.number ?: "",
                    onAction = onAction,
                    selectedTransactionType = transactionType,
                    transactionsLoading = transactionLoading,
                )
            }
        }

        if (showScrollBar) {
            state.DraggableScrollbar(
                modifier = Modifier
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .padding(horizontal = KptTheme.spacing.xs)
                    .align(Alignment.CenterEnd),
                state = scrollbarState,
                orientation = Orientation.Vertical,
                onThumbMoved = state.rememberDraggableScroller(
                    itemsAvailable = state.layoutInfo.totalItemsCount,
                ),
            )
        }

        if (showBottomSheet) {
            TransactionFilterBottomSheet(
                selectedAccount = currentSelectedAccount,
                accounts = accounts,
                selectedTransactionType = selectedTransactionType,
                onAccountSelected = {
                    onAction(HomeAction.OnFilterAccountSelected(it))
                },
                onTransactionTypeSelected = {
                    onAction(HomeAction.OnFilterTransactionTypeSelected(it))
                },
                onClearFilters = {
                    onAction(HomeAction.ClearFilters)
                },
                onApplyFilters = {
                    onAction(HomeAction.OnApplyFilterClick)
                },
                onDismiss = {
                    onAction(HomeAction.DismissBottomSheet)
                },
            )
        }
    }
}

@Composable
private fun AccountList(
    accounts: List<Account>,
    defaultAccountId: Long?,
    modifier: Modifier = Modifier,
    onMarkAsDefault: (Long, String) -> Unit,
    onClick: (Long) -> Unit,
    onPageChanged: (Int) -> Unit,
    onTopUp: () -> Unit,
) {
    val pagerState = rememberPagerState { accounts.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                onPageChanged(page)
            }
    }

    HorizontalPager(
        state = pagerState,
        pageSpacing = KptTheme.spacing.xs,
        modifier = modifier,
        contentPadding = PaddingValues(start = KptTheme.spacing.md, end = KptTheme.spacing.md),
    ) {
        AccountCard(
            account = accounts[it],
            defaultAccountId = defaultAccountId,
            onMarkAsDefault = onMarkAsDefault,
            onClick = onClick,
            onTopUp = onTopUp,
        )
    }
}

@Composable
private fun AccountCard(
    account: Account,
    defaultAccountId: Long?,
    onMarkAsDefault: (Long, String) -> Unit,
    modifier: Modifier = Modifier,
    onClick: (Long) -> Unit,
    onTopUp: () -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    val cardShape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(cardShape)
            .background(brush = tokens.ivoryGradient)
            .border(width = 1.dp, color = tokens.ivoryBorder, shape = cardShape)
            .clickable { onClick(account.id) },
    ) {
        // Signature rosettes — clipped to the card corners by the parent clip().
        Rosette(
            radiusX = 86.dp,
            radiusY = 30.dp,
            count = 40,
            opacity = 0.30f,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-46).dp),
        )
        Rosette(
            radiusX = 70.dp,
            radiusY = 22.dp,
            count = 34,
            opacity = 0.22f,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-78).dp, y = 86.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(KptTheme.spacing.lg),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = stringResource(Res.string.feature_home_wallet_balance).uppercase(),
                    style = KptTheme.typography.labelSmall.copy(
                        fontFamily = tokens.monoFontFamily,
                        letterSpacing = 2.sp,
                    ),
                    color = tokens.cardLabel,
                )

                AnimatedContent(
                    targetState = account.id == defaultAccountId,
                ) {
                    if (it) {
                        MifosSmallChip(
                            label = "Default",
                            containerColor = tokens.jade,
                        )
                    } else {
                        CardDropdownBox(
                            tint = tokens.cardLabel,
                            onClickDefault = {
                                onMarkAsDefault(account.id, account.number)
                            },
                        )
                    }
                }
            }

            val accountBalance = MoneyFormat.zar(account.balance)

            Text(
                text = accountBalance,
                color = tokens.cardInk,
                style = KptTheme.typography.headlineLarge.copy(
                    fontFamily = tokens.monoFontFamily,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SavingsChip(label = WalletNaming.friendly(account.name))
                Text(
                    text = "•••• ${account.number.takeLast(4)}",
                    style = KptTheme.typography.bodyMedium.copy(
                        fontFamily = tokens.monoFontFamily,
                    ),
                    color = tokens.cardLabel,
                )
            }
        }

        // Zero-balance prompt: overlay a green "Top up wallet" pill on the card.
        if (account.balance == 0.0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = KptTheme.spacing.lg)
                    .clip(RoundedCornerShape(50))
                    .background(tokens.jade)
                    .clickable(onClick = onTopUp)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Top up wallet",
                    style = KptTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = Color(0xFFFBFAF6),
                )
            }
        }
    }
}

@Composable
private fun SavingsChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(tokens.jadeTint)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(
            text = label,
            style = KptTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tokens.jade,
        )
    }
}

@Composable
fun CardDropdownBox(
    onClickDefault: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = KptTheme.colorScheme.surface,
) {
    var showDropdown by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = {
                showDropdown = !showDropdown
            },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = tint,
            ),
        ) {
            Icon(
                imageVector = MifosIcons.MoreVert,
                contentDescription = stringResource(Res.string.feature_home_view_more),
            )
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.feature_home_mark_default)) },
                onClick = {
                    onClickDefault()
                    showDropdown = false
                },
            )
        }
    }
}

@Composable
private fun PayRequestScreen(
    onRequest: () -> Unit,
    onSend: () -> Unit,
    onBuy: () -> Unit,
    onTopUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ActionTile(
            modifier = Modifier.weight(1f),
            text = "Send Money",
            onClick = onSend,
            icon = {
                Icon(
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer(rotationZ = 180f),
                    imageVector = vectorResource(Res.drawable.arrow_backward),
                    contentDescription = "Send Money",
                    tint = SimpliPayTheme.tokens.jade,
                )
            },
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            text = "Receive Money",
            onClick = onRequest,
            icon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = vectorResource(Res.drawable.arrow_backward),
                    contentDescription = "Receive Money",
                    tint = SimpliPayTheme.tokens.jade,
                )
            },
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            text = "Buy VAS",
            onClick = onBuy,
            icon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = MifosIcons.Storefront,
                    contentDescription = "Buy VAS",
                    tint = SimpliPayTheme.tokens.jade,
                )
            },
        )
        ActionTile(
            modifier = Modifier.weight(1f),
            text = "Top-Up Wallet",
            onClick = onTopUp,
            icon = {
                Icon(
                    modifier = Modifier.size(22.dp),
                    imageVector = MifosIcons.Wallet,
                    contentDescription = "Top-Up Wallet",
                    tint = SimpliPayTheme.tokens.jade,
                )
            },
        )
    }
}

@Composable
private fun ActionTile(
    text: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = SimpliPayTheme.tokens
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(KptTheme.colorScheme.surface)
            .border(width = 1.dp, color = tokens.border, shape = RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(tokens.jadeTint),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Text(
            text = text,
            style = KptTheme.typography.labelMedium,
            color = tokens.ink,
        )
    }
}

@Composable
private fun HomeScreenDialog(
    dialogState: HomeState.DialogState?,
    onDismissRequest: () -> Unit,
) {
    when (dialogState) {
        is HomeState.DialogState.Error -> MifosBasicDialog(
            visibilityState = BasicDialogState.Shown(
                message = dialogState.message,
            ),
            onDismissRequest = onDismissRequest,
        )

        is HomeState.DialogState.Loading -> MifosLoadingDialog(
            visibilityState = LoadingDialogState.Shown,
        )

        null -> Unit
    }
}

@Composable
private fun HomeTransactionHistoryCard(
    transactionsLoading: Boolean,
    selectedTransactionType: TransactionType,
    selectedAccount: String,
    transactions: List<Transaction>?,
    modifier: Modifier = Modifier,
    onAction: (HomeAction) -> Unit,
) {
    val tokens = SimpliPayTheme.tokens
    val cardShape = RoundedCornerShape(18.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = tokens.border, shape = cardShape),
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = KptTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KptTheme.spacing.md),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Row {
                        Text(
                            text = stringResource(Res.string.home_transaction_history),
                            style = KptTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = tokens.ink,
                        )
                        Spacer(Modifier.width(KptTheme.spacing.xs))
                        Icon(
                            imageVector = MifosIcons.OpenInNew,
                            contentDescription = "See all transactions",
                            modifier = Modifier.size(16.dp).clickable {
                                onAction(HomeAction.OnClickSeeAllTransactions)
                            },
                        )
                    }
                    Text(
                        text = stringResource(Res.string.feature_home_account_number, selectedAccount),
                        style = KptTheme.typography.bodySmall.copy(
                            fontFamily = tokens.monoFontFamily,
                        ),
                        color = tokens.sub,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 1.dp,
                            color = tokens.border,
                            shape = KptTheme.shapes.medium,
                        )
                        .clip(KptTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = {
                            onAction(
                                HomeAction.ShowBottomSheet,
                            )
                        },
                        modifier = Modifier.matchParentSize(),
                    ) {
                        Icon(
                            imageVector = MifosIcons.Filter,
                            contentDescription = "Filter transactions",
                            tint = KptTheme.colorScheme.onSurface,
                        )
                    }

                    if (selectedTransactionType != TransactionType.OTHER) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 8.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(KptTheme.colorScheme.error),
                        )
                    }
                }
            }

            transactions?.forEachIndexed { i, transaction ->
                TransactionItem(
                    transaction = transaction,
                    onClick = { accountId, transactionId ->
                        onAction(HomeAction.TransactionClicked(accountId, transactionId))
                    },
                    showLeadingIcon = false,
                )

                if (i != transactions.size - 1) {
                    MifosDivider(
                        modifier = Modifier.padding(horizontal = KptTheme.spacing.sm),
                    )
                }
            }

            if (transactionsLoading) {
                MifosProgressIndicatorMini()
            } else {
                if (transactions != null && transactions.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.home_no_transactions_found),
                        style = KptTheme.typography.bodyMedium,
                        modifier = Modifier.padding(KptTheme.spacing.md),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun HomeScreenContentPreview() {
    val accounts = listOf(
        Account(
            name = "Account 1",
            number = "123456789",
            balance = 1000.0,
            id = 1L,
            currency = Currency(
                code = "USD",
                name = "US Dollar",
                decimalPlaces = 2,
                displaySymbol = "$",
                nameCode = "USD",
                displayLabel = "US Dollar ($)",
            ),
            status = Status(
                id = 300,
                code = "status.active",
                value = "Active",
                submittedAndPendingApproval = false,
                approved = false,
                rejected = false,
                withdrawnByApplicant = false,
                active = true,
                closed = false,
                prematureClosed = false,
                transferInProgress = false,
                transferOnHold = false,
                matured = false,
            ),
        ),
        Account(
            name = "Account 1",
            number = "123456789",
            balance = 1000.0,
            id = 1L,
            currency = Currency(
                code = "USD",
                name = "US Dollar",
                decimalPlaces = 2,
                displaySymbol = "$",
                nameCode = "USD",
                displayLabel = "US Dollar ($)",
            ),
            status = Status(
                id = 300,
                code = "status.active",
                value = "Active",
                submittedAndPendingApproval = false,
                approved = false,
                rejected = false,
                withdrawnByApplicant = false,
                active = true,
                closed = false,
                prematureClosed = false,
                transferInProgress = false,
                transferOnHold = false,
                matured = false,
            ),
        ),
    )
    val transactions = listOf(
        Transaction(
            accountId = 1L,
            amount = 100.0,
            date = "2023-01-01",
            currency = Currency(
                code = "USD",
                name = "US Dollar",
                decimalPlaces = 2,
                displaySymbol = "$",
                nameCode = "USD",
                displayLabel = "US Dollar ($)",
            ),
            transactionType = TransactionType.CREDIT,
            transactionId = 101L,
            accountNo = "123456789",
            transferId = null,
            originalTransactionId = 101L,
            paymentDetailId = null,
            reversed = true,
        ),
        Transaction(
            accountId = 2L,
            amount = 100.0,
            date = "2023-01-01",
            currency = Currency(
                code = "USD",
                name = "US Dollar",
                decimalPlaces = 2,
                displaySymbol = "$",
                nameCode = "USD",
                displayLabel = "US Dollar ($)",
            ),
            transactionType = TransactionType.DEBIT,
            transactionId = 101L,
            accountNo = "123456789",
            transferId = null,
            originalTransactionId = 101L,
            paymentDetailId = null,
            reversed = false,
        ),
        Transaction(
            accountId = 3L,
            amount = 100.0,
            date = "2023-01-01",
            currency = Currency(
                code = "USD",
                name = "US Dollar",
                decimalPlaces = 2,
                displaySymbol = "$",
                nameCode = "USD",
                displayLabel = "US Dollar ($)",
            ),
            transactionType = TransactionType.CREDIT,
            transactionId = 101L,
            accountNo = "123456789",
            transferId = null,
            originalTransactionId = 101L,
            paymentDetailId = null,
            reversed = true,
        ),
        Transaction(
            accountId = 4L,
            amount = 100.0,
            date = "2023-01-01",
            currency = Currency(
                code = "USD",
                name = "US Dollar",
                decimalPlaces = 2,
                displaySymbol = "$",
                nameCode = "USD",
                displayLabel = "US Dollar ($)",
            ),
            transactionType = TransactionType.DEBIT,
            transactionId = 101L,
            accountNo = "123456789",
            transferId = null,
            originalTransactionId = 101L,
            paymentDetailId = null,
            reversed = false,
        ),
    )
    MaterialTheme {
        HomeScreenContent(
            defaultAccountId = 1L,
            onAction = {},
            transactions = transactions,
            accounts = accounts,
            showBottomSheet = false,
            selectedTransactionType = TransactionType.OTHER,
            currentSelectedAccount = null,
            transactionType = TransactionType.CREDIT,
            selectedAccount = null,
            transactionLoading = false,
        )
    }
}

/** Headless render-harness entry point (themed, side-effect free). */
@Composable
fun HomeRenderPreview() {
    val currency = Currency(
        code = "ZAR",
        name = "South African Rand",
        decimalPlaces = 2,
        displaySymbol = "R",
        nameCode = "ZAR",
        displayLabel = "South African Rand (R)",
    )
    val status = Status(
        id = 300,
        code = "status.active",
        value = "Active",
        submittedAndPendingApproval = false,
        approved = false,
        rejected = false,
        withdrawnByApplicant = false,
        active = true,
        closed = false,
        prematureClosed = false,
        transferInProgress = false,
        transferOnHold = false,
        matured = false,
    )
    val account = Account(
        name = "Everyday Savings",
        number = "100045567",
        balance = 12480.50,
        id = 1L,
        currency = currency,
        status = status,
    )
    val transactions = listOf(
        Transaction(
            accountId = 1L, amount = 2500.0, date = "12 Jun 2026", currency = currency,
            transactionType = TransactionType.CREDIT, transactionId = 101L, accountNo = "100045567",
            transferId = null, originalTransactionId = 101L, paymentDetailId = null, reversed = false,
        ),
        Transaction(
            accountId = 1L, amount = 349.99, date = "11 Jun 2026", currency = currency,
            transactionType = TransactionType.DEBIT, transactionId = 102L, accountNo = "100045567",
            transferId = null, originalTransactionId = 102L, paymentDetailId = null, reversed = false,
        ),
        Transaction(
            accountId = 1L, amount = 1200.0, date = "09 Jun 2026", currency = currency,
            transactionType = TransactionType.CREDIT, transactionId = 103L, accountNo = "100045567",
            transferId = null, originalTransactionId = 103L, paymentDetailId = null, reversed = false,
        ),
    )
    MifosTheme(darkTheme = false) {
        HomeScreenContent(
            defaultAccountId = 1L,
            onAction = {},
            transactions = transactions,
            accounts = listOf(account),
            showBottomSheet = false,
            selectedTransactionType = TransactionType.OTHER,
            currentSelectedAccount = account,
            transactionType = TransactionType.OTHER,
            selectedAccount = account,
            transactionLoading = false,
        )
    }
}
