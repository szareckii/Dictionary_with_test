package com.szareckii.dictionary.view.main

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.szareckii.core.BaseActivity
import com.szareckii.dictionary.R
import com.szareckii.dictionary.di.injectDependencies
import com.szareckii.dictionary.utils.SearchResultParser
//import com.szareckii.dictionary.utils.convertMeaningsToString
import com.szareckii.dictionary.view.description.DescriptionActivity
import com.szareckii.dictionary.view.main.adapter.MainAdapter
import com.szareckii.model.data.AppState
import com.szareckii.model.data.userdata.DataModel
import com.szareckii.utils.ui.viewById
import org.koin.android.ext.android.inject
//import kotlinx.android.synthetic.main.activity_main.*
import org.koin.android.scope.currentScope

class   MainActivity : BaseActivity<AppState, MainInteractor>() {

    override val layoutRes = R.layout.activity_main
    override lateinit var model: MainViewModel

    private lateinit var splitInstallManager: SplitInstallManager

    private val adapter: MainAdapter by lazy { MainAdapter(onListItemClickListener) }

    private lateinit var appUpdateManager: AppUpdateManager

    private val mainActivityRecyclerView by viewById<RecyclerView>(R.id.main_activity_recyclerview)
    private val searchFAB by viewById<FloatingActionButton>(R.id.search_fab)

    private val searchResultParser = SearchResultParser()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        iniViewModel()
        initViews()
        checkForUpdates()
    }

    private fun hideUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }

    // Слушатель получает от адаптера необходимые данные и запускает новый экран
    private val onListItemClickListener: MainAdapter.OnListItemClickListener =
        object : MainAdapter.OnListItemClickListener {
            override fun onItemClick(data: DataModel) {
                startActivity(
                    DescriptionActivity.getIntent(
                        this@MainActivity,
                        data.text!!,
                        searchResultParser.convertMeaningsToString(data.meanings!!),
                        data.meanings!![0].imageUrl,
                        data.meanings!![0].transcription!!
                    )
                )
            }
        }

    private val fabClickListener: View.OnClickListener =
        View.OnClickListener {
            val searchDialogFragment = SearchDialogFragment.newInstance()
            searchDialogFragment.setOnSearchClickListener(onSearchClickListener)
            searchDialogFragment.show(supportFragmentManager, BOTTOM_SHEET_FRAGMENT_DIALOG_TAG)
        }

    private val onSearchClickListener: SearchDialogFragment.OnSearchClickListener =
        object : SearchDialogFragment.OnSearchClickListener {
            override fun onClick(searchWord: String) {
                if (isNetworkAvailable) {
                    model.getData(searchWord, isNetworkAvailable)
                } else {
                    showNoInternetConnectionDialog()
                }
            }
        }

    private fun iniViewModel() {
        check(mainActivityRecyclerView.adapter == null) { getString(R.string.viewmodel_is_null) }
        injectDependencies()

        val viewModel: MainViewModel by currentScope.inject()
        model = viewModel
        model.subscribe().observe(this@MainActivity, Observer<AppState> {
            renderData(it)
        })
    }

    private fun initViews() {
        searchFAB.setOnClickListener(fabClickListener)

        mainActivityRecyclerView.layoutManager = LinearLayoutManager(applicationContext)
        mainActivityRecyclerView.adapter = adapter
    }

    override fun setDataToAdapter(data: List<DataModel>) {
        adapter.setData(data)
    }

    override fun showWordLocalRep(data: List<DataModel>) {
        startActivity(
            DescriptionActivity.getIntent(
                this@MainActivity,
                data[0].text!!,
                "desc",
                data[0].meanings?.get(0)?.imageUrl,
                "trans"
            )
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_history -> {
                splitInstallManager = SplitInstallManagerFactory.create(applicationContext)
                val request =
                    SplitInstallRequest
                        .newBuilder()
                        .addModule(HISTORY_ACTIVITY_FEATURE_NAME)
                        .build()

                splitInstallManager
                    .startInstall(request)
                    .addOnSuccessListener {
                        val intent = Intent().setClassName(packageName, HISTORY_ACTIVITY_PATH)
                        startActivity(intent)
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            applicationContext,
                            "Couldn't download feature: " + it.message,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                true
            }

            R.id.menu_screen_settings -> {
                startActivityForResult(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY), 42)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun checkForUpdates() {
        // Создаём менеджер
        appUpdateManager = AppUpdateManagerFactory.create(applicationContext)
        // Возвращает интент (appUpdateInfo), который мы будем использовать
        // в качестве информации для обновления
        val appUpdateInfo = appUpdateManager.appUpdateInfo

        // Проверяем наличие обновления
        appUpdateInfo.addOnSuccessListener { appUpdateIntent ->
            if (appUpdateIntent.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                // Здесь мы делаем проверку на немедленный тип обновления
                // (IMMEDIATE); для гибкого нужно передавать AppUpdateType.FLEXIBLE
                && appUpdateIntent.isUpdateTypeAllowed(IMMEDIATE)
            ) {
                // Передаём слушатель прогресса (только для гибкого типа
                // обновления)
                appUpdateManager.registerListener(stateUpdatedListener)
                // Выполняем запрос
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateIntent,
                    IMMEDIATE,
                    this,
                    // Реквест-код для обработки запроса в onActivityResult
                    REQUEST_CODE
                )
            }
        }
    }

    private val stateUpdatedListener: InstallStateUpdatedListener = object : InstallStateUpdatedListener {

        override fun onStateUpdate(state: InstallState?) {
            // Переменная state позволяет следить за прогрессом установки
            state?.let {
                if (it.installStatus() == InstallStatus.DOWNLOADED) {
                    // Когда обновление скачалось и готово к установке, отображаем
                    // SnackBar
                    popupSnackbarForCompleteUpdate()
                }
            }
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            findViewById(R.id.activity_main_layout),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                // Если всё в порядке, снимаем слушатель прогресса обновления
                appUpdateManager.unregisterListener(stateUpdatedListener)
            } else {
                // Если обновление прервано (пользователь не принял или прервал
                // его) или не загружено (из-за проблем с соединением), показываем
                // уведомление (также можно показать диалоговое окно с предложением
                // попробовать обновить еще раз)
                Toast.makeText(
                        applicationContext,
                        "Update flow failed! Result code: $resultCode",
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager
                .appUpdateInfo
                .addOnSuccessListener { appUpdateInfo ->
                    // Обновление скачано, но не установлено
                    if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    }
                    if (appUpdateInfo.updateAvailability()
                            == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                    ) {
                        // Обновление прервано - можно возобновить установку
                        appUpdateManager.startUpdateFlowForResult(
                                appUpdateInfo,
                                AppUpdateType.IMMEDIATE,
                                this,
                                REQUEST_CODE
                        )
                    }
                }
        hideUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)
        // Находим нужную кнопку и меняем ей видимость в зависимости от OS
        menu?.findItem(R.id.menu_screen_settings)?.isVisible =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        return super.onCreateOptionsMenu(menu)
    }

    companion object {
        private const val BOTTOM_SHEET_FRAGMENT_DIALOG_TAG = "74a54328-5d62-46bf-ab6b-cbf5fgt0-092395"
        private const val HISTORY_ACTIVITY_PATH = "com.szareckii.historyscreen.view.history.HistoryActivity"
        private const val HISTORY_ACTIVITY_FEATURE_NAME = "historyscreen"
        private const val REQUEST_CODE = 100
    }
}
