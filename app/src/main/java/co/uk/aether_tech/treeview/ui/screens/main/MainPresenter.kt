package co.uk.aether_tech.treeview.ui.screens.main

import co.uk.aether_tech.treeview.models.Tree

class MainPresenter private constructor(private val view: MainContract.View) : MainContract.Presenter {
    companion object {
        @JvmStatic
        fun createAndAttachProducer(view: MainContract.View): MainPresenter {
            val presenter = MainPresenter(view)
            presenter.view.setPresenter(presenter)
            return presenter
        }
    }

    override fun start() {
        view.treeView.tree = Tree("+")
    }

    override fun addClick() {
        view.output = ""
        if (view.treeView.selectedNode != null) {
            view.treeView.selectedNode?.add (Tree(view.title))
        } else {
            view.treeView.tree?.add(Tree(view.title))
        }
        view.treeView.invalidate()
    }

    override fun doneClick() {
        addClick()
    }

    override fun removeClick() {
        if (view.treeView.selectedNode != null) {
            view.treeView.tree?.remove(view.treeView.selectedNode)
            view.treeView.selectedNode = null
            view.treeView.invalidate()
        }
    }

    override fun nodeSelected(node: Tree?) {
        view.title = node?.text ?: ""
    }

    override fun onResetClick() {
        view.treeView.resizeToFit()
    }
}