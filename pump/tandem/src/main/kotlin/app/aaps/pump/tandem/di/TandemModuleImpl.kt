package app.aaps.pump.tandem.di

// import app.aaps.pump.tandem.common.comm.ui.TandemUIDataStore
// import app.aaps.pump.tandem.common.driver.tandemUiDataStore
// import dagger.Module
// import dagger.Provides
// import dagger.hilt.InstallIn
// import dagger.hilt.components.SingletonComponent
// import javax.inject.Singleton
//
// @Module
// @InstallIn(SingletonComponent::class)
// @Suppress("unused")
// open class TandemModuleImpl {
//
//     // Returns the single global instance (the same one backing the tandemDataStore write-only
//     // handle and the LocalTandemDataStore composition local), so Dagger-injected consumers
//     // (e.g. TandemUICommunication) share it rather than getting a second instance.
//     @Provides
//     @Singleton
//     fun provideTandemUIDataStore(): TandemUIDataStore = tandemUiDataStore
// }