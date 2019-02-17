package co.uk.aether_tech.treeview.ui.screens.base

interface BaseView<T: BasePresenter> {
    fun setPresenter(presenter: T)
}