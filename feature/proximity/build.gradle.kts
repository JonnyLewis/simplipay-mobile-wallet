/*
 * Copyright 2024 Mifos Initiative
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * See https://github.com/openMF/mobile-wallet/blob/master/LICENSE.md
 */
plugins {
    alias(libs.plugins.cmp.feature.convention)
}

android {
    namespace = "org.mifospay.feature.proximity"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.data)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        androidMain.dependencies {
            // androidContext() for the Android BLE transport (BluetoothManager).
            implementation(libs.koin.android)
        }

        commonTest.dependencies {
            implementation(libs.turbine)
        }
    }
}
