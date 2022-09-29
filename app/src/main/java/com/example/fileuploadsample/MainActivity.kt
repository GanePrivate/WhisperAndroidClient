package com.example.fileuploadsample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.fileuploadsample.UriToFileUtil.toFile
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity(), FileSelectionDialog.OnFileSelectListener {
    private val MENUID_FILE = 0     // オプションメニューID
    private lateinit var m_strInitialDir: File       // 初期フォルダ
    private val PERMISSION_WRITE_EX_STR = 1
    private var selectedItem = ""

    // モデル一覧
    private val spinnerItems = arrayOf("tiny", "base", "small", "medium", "large")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ArrayAdapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerItems)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelList.adapter = adapter  // spinner に adapter をセット
        // smallのモデルをデフォルトで選択しておく
        modelList.setSelection(2)

        // リスナーを登録
        modelList.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            //　アイテムが選択された時
            override fun onItemSelected(parent: AdapterView<*>?,
                                        view: View?, position: Int, id: Long) {
                val spinnerParent = parent as Spinner
                selectedItem = spinnerParent.selectedItem as String
                Log.d("selectedItem", selectedItem)
            }

            //　アイテムが選択されなかった
            override fun onNothingSelected(parent: AdapterView<*>?) {
                //
            }
        }

        // Android10(API29) 未満の場合は外部ストレージアクセスのパーミッション許可ポップアップ表示
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                val permissions = arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                ActivityCompat.requestPermissions(this@MainActivity, permissions, 0)
            }
        }

        if (Build.VERSION.SDK_INT in 23..29) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_CONTACTS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_CONTACTS
                    ),
                    PERMISSION_WRITE_EX_STR
                )
            }
        }

        // 権限の取得ボタンが押された時の処理
        get_Permission_button.setOnClickListener {
            val intent = Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION")
            startActivity(intent)
        }

        // UPLOADボタンが押された時の処理
        ul_button.setOnClickListener {
            selectFile()
        }
    }


    // ファイルを選択するダイアログを表示する
    fun selectFile() {
        //外部ストレージ　ルートフォルダパス取得
        var externalFilesDirs =
            this@MainActivity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!.path.split(
                "/"
            )
        var externalFilesDirsPath = ""
        var i = 0
        externalFilesDirs.forEach { externalFilesDirsPeart ->
            if (i > 3) {
                return@forEach
            }
            if (externalFilesDirsPeart != "") {
                externalFilesDirsPath = externalFilesDirsPath + "/" + externalFilesDirsPeart
            }

            i++
        }

        //外部ストレージ　ダウンロードフォルダパスを初期フォルダとして変数に保存
        m_strInitialDir = File(externalFilesDirsPath)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // ファイル選択ダイアログ表示　Android 9.0　(API 28)　以下の場合の処理
            // アプリが minSdkVersion 26　なのでそれ以下の端末処置は考慮していない
            val dlg = FileSelectionDialog(this, this)
            dlg.show(m_strInitialDir)
        } else {
            // ファイル選択Activity表示　Android 9.0　(API 28)　を超えるの場合の処理
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "audio/*"
            startActivityForResult(intent, MENUID_FILE)
        }
    }


    // Android 9.0　(API 28) 以下でファイルが選択されたときに呼び出される関数
    override fun onFileSelect(file: File?) {
        Toast.makeText(
            this,
            "API:" + Build.VERSION.SDK_INT + " ファイルが選択されました。\n : " + file!!.getPath(),
            Toast.LENGTH_LONG
        ).show()

        // ファイルパスを指定してアップロードを実行
        responseView.text = "解析中"
        val postData = PostData(fileName=file.name, filePath=file.getPath(), modelName=selectedItem)
        postData.run(callback = object : ApiResult {

            // アップロード完了時の処理
            override fun onSuccess(res: String) {
                responseView.text = res.substring(9, res.length-1)
            }

            // アップロード失敗時の処理
            override fun onError(res: String?) {
                responseView.text = res
            }
        })
    }


    //Android 9.0　(API 28)　を超える端末でファイルが選択されたときに呼び出される関数
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                MENUID_FILE -> {
                    // ファイル名を取得
                    var selectFileName = "ファイル名を取得できませんでした"
                    // ファイルURIを取得
                    val uri: Uri = data?.data!!
                    // ファイルURIをファイルパスに変換
                    val filePath: String = toFile(this, uri).path
                    data!!.data?.let { selectFileUri ->
                        contentResolver.query(selectFileUri, null, null, null, null)
                    }?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        cursor.moveToFirst()
                        selectFileName = cursor.getString(nameIndex)
                    }

                    // トーストの表示
                    Toast.makeText(
                        this,
                        "API:" + Build.VERSION.SDK_INT + " ファイルが選択されました。\n : " + selectFileName,
                        Toast.LENGTH_LONG
                    ).show()

                    // ファイルパスを指定してアップロードを実行
                    responseView.text = "解析中..."
                    val postData = PostData(fileName=selectFileName, filePath=filePath, modelName=selectedItem)
                    postData.run(callback = object : ApiResult {

                        // アップロード完了時の処理
                        override fun onSuccess(res: String) {
                            responseView.text = res.substring(9, res.length-1)
                        }

                        // アップロード失敗時の処理
                        override fun onError(res: String?) {
                            responseView.text = res
                        }
                    })
                }
                else -> {/*何もしない*/
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}