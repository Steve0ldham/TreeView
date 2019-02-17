package co.uk.aether_tech.treeview.ui.screens.main

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.uk.aether_tech.treeview.R
import co.uk.aether_tech.treeview.ui.views.TreeView

class MainActivity : AppCompatActivity(), MainContract.View {
    private lateinit var presenter: MainContract.Presenter
    private lateinit var txtTitle: EditText
    private lateinit var txtOutput: TextView

    override var title: String
        get() = txtTitle.text.toString()
        set(value) {txtTitle.setText(value) }

    override var output: String
        get() = txtOutput.text.toString()
        set(value) {txtOutput.text = value}

    override lateinit var treeView: TreeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        MainPresenter.createAndAttachProducer(this)
    }

    override fun onStart() {
        super.onStart()
        presenter.start()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.mnuReset -> presenter.onResetClick()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        txtTitle = findViewById(R.id.txtTitle)
        txtTitle.setOnEditorActionListener {
            _, action, _ -> if (action == EditorInfo.IME_ACTION_DONE) {
            presenter.doneClick()
        }
            false
        }
        txtOutput = findViewById(R.id.txtOutput)
        treeView = findViewById(R.id.tree)
        findViewById<Button>(R.id.btnDone).setOnClickListener{ presenter.addClick() }
        findViewById<Button>(R.id.btnRemove).setOnClickListener{ presenter.removeClick() }
        treeView.onNodeSelected = { presenter.nodeSelected(it) }
    }

    override fun setPresenter(presenter: MainContract.Presenter) {
        this.presenter = presenter
    }

    override fun showSnackbar(message: String) {
        Snackbar.make(txtTitle, message, Snackbar.LENGTH_LONG).show()
    }
}
