package com.goldenv2.feature.home

import com.goldenv2.feature.home.ads.AdmobAdProvider
import com.goldenv2.feature.home.ads.AdsManager
import com.goldenv2.feature.home.ads.ConsentManager
import com.goldenv2.feature.home.ads.YandexAdProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HomeModule {
    // ViewModels are provided by Hilt automatically via @HiltViewModel

    /**
     * Ad networks in fallback order: AdMob first, Yandex Mobile Ads as fallback.
     * Expressed as a list so [AdsManager] decides at runtime which one can serve.
     */
    @Provides
    @Singleton
    fun provideAdsManager(
        consentManager: ConsentManager,
        admobProvider: AdmobAdProvider,
        yandexProvider: YandexAdProvider
    ): AdsManager = AdsManager(consentManager, listOf(admobProvider, yandexProvider))
}