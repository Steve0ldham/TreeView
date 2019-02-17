package co.uk.aether_tech.treeview.ui.screens.main

import co.uk.aether_tech.treeview.models.Tree
import co.uk.aether_tech.treeview.ui.screens.base.BasePresenter
import co.uk.aether_tech.treeview.ui.screens.base.BaseView
import co.uk.aether_tech.treeview.ui.views.TreeView

interface MainContract {
    interface View: BaseView<Presenter> {
        var title: String
        var output: String
        var treeView: TreeView

        fun showSnackbar(message: String)
    }

    interface Presenter: BasePresenter {
        fun addClick()
        fun nodeSelected(node: Tree?)
        fun doneClick()
        fun removeClick()
        fun onResetClick()
    }
}