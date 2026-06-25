/*
 * Copyright 2026 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
package org.mifospay.passcode

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Vendored from openMF/mifos-passcode-cmp (MPL-2.0), with the keypad-input methods
 * (`enterKey`, `deleteKey`, `setResultCallback`, …) promoted from `internal` to `public`
 * so the SimpliPay-styled [SimpliPayPasscodeScreen] can drive it. The state-machine logic
 * is otherwise unchanged from upstream.
 */
class PasscodeManager(
    private val adapter: PasscodeStorageAdapter,
) {
    private val _state = MutableStateFlow(
        PasscodeState(
            loadedPasscode = adapter.loadPasscode(),
        ),
    )

    val state = _state.asStateFlow()

    private var onResult: ((PasscodeResult) -> Unit)? = null

    private val creationPasscodeBuilder = StringBuilder()
    private val finalConfirmationPasscodeBuilder = StringBuilder()

    init {
        val loaded = adapter.loadPasscode()
        when {
            loaded != null -> {
                updateState { it.copy(loadedPasscode = loaded, passcodeStep = PasscodeStep.Enter) }
                updatePasscodeLength(
                    if (loaded.length == 6) PasscodeLength.SIX_DIGIT else PasscodeLength.FOUR_DIGIT,
                )
            }
            else -> updateState { it.copy(passcodeStep = PasscodeStep.Create) }
        }
    }

    fun changePasscode() {
        updateState { it.copy(passcodeStep = PasscodeStep.ChangeVerify, isChangeFlow = true) }
    }

    fun logOut() = clearAllSecurityData()

    fun setResultCallback(callback: ((PasscodeResult) -> Unit)?) {
        onResult = callback
    }

    fun forgetPasscode() {
        clearAllSecurityData()
        emitResult(PasscodeResult.Forgotten)
    }

    fun updatePasscodeLength(length: PasscodeLength) {
        updateState { it.copy(passcodeLength = length) }
    }

    fun deleteKey() {
        val passcodeBuilder = getActivePasscodeBuilder()
        if (passcodeBuilder.isNotEmpty()) {
            passcodeBuilder.deleteAt(passcodeBuilder.length - 1)
            updateState {
                it.copy(
                    currentPasscodeInput = passcodeBuilder.toString(),
                    filledDots = passcodeBuilder.length,
                )
            }
        }
    }

    fun deleteAllKeys() {
        getActivePasscodeBuilder().clear()
        updateState { it.copy(currentPasscodeInput = "", filledDots = 0) }
    }

    fun enterKey(key: String) {
        val currentState = _state.value
        if (currentState.filledDots >= _state.value.passcodeLength.length) return

        val passcodeBuilder = getActivePasscodeBuilder()
        passcodeBuilder.append(key)

        updateState {
            it.copy(
                currentPasscodeInput = passcodeBuilder.toString(),
                filledDots = passcodeBuilder.length,
            )
        }

        if (passcodeBuilder.length == _state.value.passcodeLength.length) {
            handleCompletedPasscodeEntry()
        }
    }

    fun togglePasscodeVisibility() {
        updateState { it.copy(passcodeVisible = !_state.value.passcodeVisible) }
    }

    private fun handleCompletedPasscodeEntry() {
        when (_state.value.passcodeStep) {
            PasscodeStep.ChangeVerify -> handleChangeVerifyPasscode()
            PasscodeStep.Enter -> handleEnterPasscode()
            PasscodeStep.Create -> handleCreatePasscode()
            PasscodeStep.Confirm -> handleConfirmPasscode()
            else -> {}
        }
    }

    private fun getActivePasscodeBuilder(): StringBuilder {
        return when (_state.value.passcodeStep) {
            PasscodeStep.ChangeVerify,
            PasscodeStep.Confirm,
            PasscodeStep.Enter,
            -> finalConfirmationPasscodeBuilder
            else -> creationPasscodeBuilder
        }
    }

    private fun emitResult(result: PasscodeResult) {
        onResult?.invoke(result)
    }

    private fun handleChangeVerifyPasscode() {
        val loadedPasscode = adapter.loadPasscode()
        if (finalConfirmationPasscodeBuilder.toString() == loadedPasscode) {
            updateState {
                it.copy(
                    passcodeStep = PasscodeStep.Create,
                    passcodeLength = when (loadedPasscode.length) {
                        6 -> PasscodeLength.SIX_DIGIT
                        else -> PasscodeLength.FOUR_DIGIT
                    },
                    isChangeFlow = true,
                )
            }
        } else {
            updateState { it.copy(shakeAnimationTrigger = it.shakeAnimationTrigger + 1) }
            emitResult(PasscodeResult.Rejected)
        }
        resetPasscodeEntryStates()
    }

    private fun handleCreatePasscode() {
        updateState {
            it.copy(
                currentPasscodeInput = "",
                filledDots = 0,
                passcodeVisible = false,
                passcodeStep = PasscodeStep.Confirm,
            )
        }
    }

    private fun handleConfirmPasscode() {
        val newPasscode = finalConfirmationPasscodeBuilder.toString()
        if (creationPasscodeBuilder.toString() == newPasscode) {
            adapter.savePasscode(newPasscode)
            val isChange = _state.value.isChangeFlow
            updateState {
                it.copy(
                    currentPasscodeInput = "",
                    filledDots = 0,
                    passcodeVisible = false,
                    passcodeStep = PasscodeStep.Enter,
                    loadedPasscode = newPasscode,
                    isChangeFlow = false,
                )
            }
            creationPasscodeBuilder.clear()
            if (isChange) emitResult(PasscodeResult.Changed) else emitResult(PasscodeResult.Created)
        } else {
            updateState { it.copy(currentPasscodeInput = "", filledDots = 0) }
            updateState { it.copy(shakeAnimationTrigger = it.shakeAnimationTrigger + 1) }
        }
        resetPasscodeEntryStates()
    }

    private fun handleEnterPasscode() {
        if (finalConfirmationPasscodeBuilder.toString() == _state.value.loadedPasscode) {
            emitResult(PasscodeResult.Verified)
        } else {
            emitResult(PasscodeResult.Rejected)
            updateState { it.copy(shakeAnimationTrigger = it.shakeAnimationTrigger + 1) }
        }
        resetPasscodeEntryStates()
    }

    private fun resetPasscodeEntryStates() {
        updateState {
            it.copy(
                filledDots = 0,
                currentPasscodeInput = "",
                passcodeVisible = false,
                passcodeLength = when (_state.value.loadedPasscode?.length) {
                    6 -> PasscodeLength.SIX_DIGIT
                    else -> PasscodeLength.FOUR_DIGIT
                },
            )
        }
        finalConfirmationPasscodeBuilder.clear()
    }

    private fun clearAllSecurityData() {
        adapter.deletePasscode()
        updateState {
            it.copy(
                loadedPasscode = null,
                passcodeStep = PasscodeStep.Create,
                isChangeFlow = false,
            )
        }
        creationPasscodeBuilder.clear()
        resetPasscodeEntryStates()
    }

    private fun updateState(update: (PasscodeState) -> PasscodeState) {
        _state.update { update(it) }
    }
}

data class PasscodeState(
    val filledDots: Int = 0,
    val passcodeLength: PasscodeLength = PasscodeLength.FOUR_DIGIT,
    val passcodeVisible: Boolean = false,
    val currentPasscodeInput: String = "",
    val loadedPasscode: String? = null,
    val passcodeStep: PasscodeStep = PasscodeStep.Unset,
    val isChangeFlow: Boolean = false,
    val shakeAnimationTrigger: Int = 0,
)

sealed interface PasscodeResult {
    data object Verified : PasscodeResult
    data object Created : PasscodeResult
    data object Changed : PasscodeResult
    data object Forgotten : PasscodeResult
    data object Rejected : PasscodeResult
}

enum class PasscodeStep {
    Unset,
    Enter,
    Create,
    Confirm,
    ChangeVerify,
}
