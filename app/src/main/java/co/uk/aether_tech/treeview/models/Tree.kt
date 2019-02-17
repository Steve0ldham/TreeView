package co.uk.aether_tech.treeview.models

import android.os.Parcel
import android.os.Parcelable
import java.lang.Math
import java.util.*

class Tree (var text: String?): Parcelable {
    var x: Float = -1f
    var y: Float = -1f
    var radius: Float = -1f

    val rootNodes: MutableList<Tree> = ArrayList()

    private var index: Int = -1

    interface TraversalListener {
        fun onTraversal(node: Tree, depth: Int, height: Int, isBottom: Boolean, offset: Float)
    }

    fun add(node: Tree) : Tree {
        rootNodes.add(node)
        node.index = rootNodes.indexOf(node)
        return node
    }

    operator fun get(index: Int) : Tree {
        return rootNodes[index]
    }

    operator fun set(index: Int, node: Tree) {
        rootNodes[index] = node
    }

    fun remove(node: Tree?) {
        if (node == null) {
            return
        }
        if (rootNodes.contains(node)) {
            rootNodes.remove(node)
        } else {
            for (subNode in rootNodes) {
                subNode.remove(node)
            }
        }
    }

    fun clear() {
        for (node in rootNodes) {
            remove(node)
        }
    }

    val isLeaf: Boolean
    get() = rootNodes.size == 0


    fun doTraversal (listener: TraversalListener) {
        doTraversal(listener, 0, 0, true, 0.5f)
    }

    private fun doTraversal(listener: TraversalListener, depth: Int, height: Int, isBottom: Boolean, offset: Float) {
        listener.onTraversal(this, depth, height, isBottom, offset)
        var currentHeight: Int = height
        val branchHeight = this.subHeight
        var currentOffset = 0f
        for (i in 0 until rootNodes.size){
            val node = rootNodes[i]
            val nodeHeight = if (node.subHeight == 0) 1 else node.subHeight
            val nodeHeightRatio = if (branchHeight == 1) 0.5f else (nodeHeight / (branchHeight - 1).toFloat())
            node.doTraversal(listener, depth + 1, currentHeight, i==rootNodes.size-1, currentOffset)
            currentOffset += nodeHeightRatio * nodeHeight
            currentHeight += if (node.subHeight < 1) 1 else node.subHeight
        }
    }

    val subHeight: Int
    get() {
        var currentHeight: Int = size
        for (node in rootNodes) {
            val nodeHeight = node.subHeight
            currentHeight += if (nodeHeight > 0) nodeHeight - 1 else 0
        }
        return currentHeight
    }

    var height: Int = 0

    val subDepth: Int
    get() = getDepth(0)

    var depth: Int = 0

    private fun getDepth(depth: Int): Int {
        var subDepth = depth + 1
        if (!isLeaf) {
            subDepth = 0
            for (node in rootNodes) {
                val nodeDepth = node.getDepth(depth + 1)
                subDepth = Math.max(nodeDepth, subDepth)
            }
        }
        return subDepth
    }

    val size : Int
    get() = rootNodes.size

    constructor(parcel: Parcel) : this(parcel.readString()) {
        x = parcel.readFloat()
        y = parcel.readFloat()
        radius = parcel.readFloat()
        index = parcel.readInt()
        parcel.readTypedList(rootNodes, CREATOR)
    }

    private fun isHit(x: Float, y: Float): Boolean {
        return x > (this.x - (radius)) && x < (this.x + (radius)) && y > (this.y - (radius)) && y < (this.y + (radius))
    }

    fun findHit(x: Float, y: Float): Tree? {
        if (isHit(x, y)) {
            return this
        }
        for (node in rootNodes) {
            val selection: Tree? = node.findHit(x, y)
            if (selection != null) {
                return selection
            }
        }
        return null
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(text)
        parcel.writeFloat(x)
        parcel.writeFloat(y)
        parcel.writeFloat(radius)
        parcel.writeInt(index)
        parcel.writeTypedList(rootNodes)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Tree> {
        override fun createFromParcel(parcel: Parcel): Tree {
            return Tree(parcel)
        }

        override fun newArray(size: Int): Array<Tree?> {
            return arrayOfNulls(size)
        }
    }
}