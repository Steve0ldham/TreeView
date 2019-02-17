package co.uk.aether_tech.treeview.ui.views

import android.annotation.TargetApi
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.uk.aether_tech.treeview.R
import co.uk.aether_tech.treeview.models.Tree
import android.graphics.Color.parseColor
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.RequiresApi
import android.support.v4.view.ViewCompat
import android.util.TypedValue
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.VelocityTracker
import android.widget.OverScroller
import java.util.*


class TreeView : View {
    interface NodeSelectedListener {
        fun onNodeSelected(node: Tree?)
    }

    private var scaleGestureDetector: ScaleGestureDetector? = null

    private val defaultFillColourStates = arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf(android.R.attr.state_pressed), intArrayOf())
    private val defaultFillColours = intArrayOf(Color.parseColor("#408040"), Color.parseColor("#202080"), Color.parseColor("#404080"))
    private val defaultFillColourStateList = ColorStateList(defaultFillColourStates, defaultFillColours)
    private val defaultFrameColourStates = arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf(android.R.attr.state_pressed), intArrayOf())
    private val defaultFrameColours = intArrayOf(Color.parseColor("#404040"), Color.parseColor("#202020"), Color.parseColor("#000000"))
    private val defaultFrameColourStateList = ColorStateList(defaultFrameColourStates, defaultFrameColours)

    private val toleranceDefault = 2f

    var tree: Tree? = null
    set(value) {
        field = value
        invalidate()
    }


    private lateinit var frame: Paint
    private lateinit var fill: Paint
    private var moveTolerance: Float = toleranceDefault

    var nodeSelectedListener: NodeSelectedListener?
        get(){
            return if (onNodeSelected == null) null else object: NodeSelectedListener {
                override fun onNodeSelected(node: Tree?) {
                    this@TreeView.onNodeSelected?.invoke(node)
                }
            }
        }
        set(value) {
            if (value != null) {
                this.onNodeSelected = value::onNodeSelected
            }
            else {
                this.onNodeSelected = null
            }
        }

    private var frameColour = defaultFrameColourStateList
    private var frameWidth: Float = 2f
    private var fillColour = defaultFillColourStateList
    private var connectorColour = defaultFrameColours[0]
    private var connectorWidth: Float = 2f
    private var minNodeSize: Float = 0f

    private var isMoving: Boolean = false

    private var topY: Float = 0f
    private var leftX: Float = 0f
    private var contentHeight = 0f
    private var contentWidth = 0f
    private var velocityTracker: VelocityTracker? = null
    private var scroller: OverScroller? = null
    private var autoSize: Boolean = true
    private var nodeSize: Float = 0f

    var onNodeSelected: ((node: Tree?) -> Unit)? = null

    var selectedNode: Tree? = null
    private var pressedNode: Tree? = null

    constructor(context: Context?) : this(context, null) {
        initialise(context, null)
    }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initialise(context, attrs)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initialise(context, attrs)
    }

    @TargetApi(android.os.Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        initialise(context, attrs)
    }

    fun resizeToFit() {
        topY = 0f
        leftX = 0f
        autoSize = true
        invalidate()
    }

    private fun initialise(context: Context?, attrs: AttributeSet?) {
        val typedArray: TypedArray? = context?.obtainStyledAttributes(attrs, R.styleable.TreeView)
        if (isInEditMode) {
            val tree = Tree("Root")
            tree.add(Tree("1:1")).add(Tree("1:1:1")).add(Tree("1:1:1:1"))
            tree[0].add(Tree("1:2:1"))
            tree.add(Tree("2:1")).add(Tree("2:1:1"))
            this.tree = tree
        }
        frameWidth = typedArray?.getDimension(R.styleable.TreeView_nodeFrameWidth, resources.getDimension(R.dimen.stroke_width_default)) ?: resources.getDimension(R.dimen.stroke_width_default)
        connectorWidth = typedArray?.getDimension(R.styleable.TreeView_nodeConnectorWidth, resources.getDimension(R.dimen.stroke_width_default)) ?: resources.getDimension(R.dimen.stroke_width_default)
        minNodeSize = typedArray?.getDimension(R.styleable.TreeView_minNodeSize, resources.getDimension(R.dimen.defaultMinNodeSize)) ?: 0f

        frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = parseColor("#000000")
            style = Paint.Style.STROKE
        }

        fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#404080")
            style = Paint.Style.FILL_AND_STROKE
        }
        if (typedArray?.hasValue(R.styleable.TreeView_nodeFillColour) == true) {
            fillColour = typedArray.getColorStateList(R.styleable.TreeView_nodeFillColour) ?:
                    ColorStateList(arrayOf(intArrayOf()), intArrayOf(typedArray.getColor(R.styleable.TreeView_nodeFillColour, defaultFillColours[0])))
        }
        if (typedArray?.hasValue(R.styleable.TreeView_nodeFrameColour) == true) {
            frameColour = typedArray.getColorStateList(R.styleable.TreeView_nodeFrameColour) ?:
                    ColorStateList(arrayOf(intArrayOf()), intArrayOf(typedArray.getColor(R.styleable.TreeView_nodeFrameColour, defaultFrameColours[0])))
        }
        if (typedArray?.hasValue(R.styleable.TreeView_nodeConnectorColour) == true) {
            connectorColour = typedArray.getColor(R.styleable.TreeView_nodeConnectorColour, defaultFrameColours[0])
        }
        typedArray?.recycle()
        scroller = OverScroller(context)
        scroller?.setFriction(.01f)
        scaleGestureDetector = ScaleGestureDetector(this.context, ScaleListener())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        val viewHeight = measuredHeight
        val viewWidth = measuredWidth
        val treeDepth: Int = if (tree?.subDepth == 0) 1 else tree?.subDepth ?: 1
        val treeHeight: Int = if (tree?.subHeight == 0) 1 else tree?.subHeight ?: 1
        if (autoSize) {
            nodeSize = (Math.min(viewHeight / treeHeight, viewWidth / treeDepth)).toFloat()
        }
        if (nodeSize < minNodeSize) {
            nodeSize = minNodeSize
        }
        contentWidth = nodeSize * (treeDepth - 1)
        contentHeight = nodeSize * treeHeight
        drawNode(canvas, tree, nodeSize, (nodeSize / 2) + leftX - nodeSize, topY, 0)
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, toleranceDefault, resources.displayMetrics)
    }

    private fun drawNode(canvas: Canvas?, node: Tree?, nodeSize: Float, x: Float, y: Float, depth: Int) {
        val radius = nodeSize / 4
        var currentY = y
        if (node != null) {
            val nodeHeight = if (node.subHeight == 0) 1 else node.subHeight
            node.x = x
            node.y = y + (((nodeHeight) * nodeSize) / 2)
            node.depth = depth
            if (depth > 0) {
                frame.apply {
                    strokeWidth = connectorWidth
                    color = connectorColour
                }
                if (!node.isLeaf) {
                    canvas?.drawLine(node.x, node.y, x + (nodeSize / 2), node.y, frame)
                }
            }
            for (subNode in node.rootNodes) {
                val subNodeHeight = if (subNode.subHeight == 0) 1 else subNode.subHeight
                frame.apply {
                    strokeWidth = connectorWidth
                    color = connectorColour
                }
                val newNodeY = currentY +(((subNodeHeight * nodeSize) / 2))
                if (depth > 0) {
                    canvas?.drawLine(node.x + (nodeSize / 2) + (connectorWidth / 2), node.y, x + (nodeSize / 2) + (connectorWidth / 2), newNodeY, frame)
                    canvas?.drawLine(node.x + (nodeSize / 2), newNodeY, x + nodeSize, newNodeY, frame)
                }

                drawNode(canvas, subNode, nodeSize, x + nodeSize, currentY, depth + 1)
                currentY += nodeSize * (if (subNode.subHeight == 0) 1 else subNode.subHeight)
            }
            if (depth > 0) {
                val nodeState = ArrayList<Int>()
                if (node == selectedNode) nodeState.add(android.R.attr.state_selected)
                if (node == pressedNode) nodeState.add(android.R.attr.state_pressed)
                if (node.depth == 1) nodeState.add(android.R.attr.state_first)
                if (node.isLeaf) nodeState.add(android.R.attr.state_last)
                fill.color = getColourForState(fillColour, nodeState.toIntArray(), defaultFillColours[0])
                frame.apply {
                    color = getColourForState(frameColour, nodeState.toIntArray(), defaultFrameColours[0])
                    strokeWidth = frameWidth
                }
                node.radius = radius
                canvas?.drawCircle(node.x, node.y, radius, fill)
                canvas?.drawCircle(node.x, node.y, radius, frame)
            }
        }
    }

    private fun getColourForState (stateList: ColorStateList, state: IntArray?, defaultColour: Int) : Int {
        return stateList.getColorForState(state, defaultColour)
    }

    override fun performClick(): Boolean {
        onNodeSelected?.invoke(selectedNode)
        return super.performClick()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        scaleGestureDetector?.onTouchEvent(event)
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                if (event.pointerCount == 1) {
                    scroller?.forceFinished(true)
                    isMoving = false
                    pressedNode = tree?.findHit(event.x, event.y)
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain()
                    } else {
                        velocityTracker?.clear()
                    }
                    velocityTracker?.addMovement(event)
                    invalidate()
                    return true
                } else {
                    pressedNode = null
                    isMoving = false
                    velocityTracker?.recycle()
                    velocityTracker = null
                    invalidate()
                    return false
                }
            }
            MotionEvent.ACTION_UP -> {
                val node: Tree? = tree?.findHit(event.x, event.y)
                if (node == pressedNode) {
                    selectedNode = tree?.findHit(event.x, event.y)
                    performClick()
                }
                velocityTracker?.computeCurrentVelocity(500)
                val xVelocity = (velocityTracker?.xVelocity ?: 0f) * -1
                val yVelocity = (velocityTracker?.yVelocity ?: 0f) * -1
                handleStop(xVelocity, yVelocity)
                velocityTracker?.recycle()
                velocityTracker = null
                pressedNode = null
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount == 1 && pressedNode == null) {
                    val historySize = event.historySize
                    if (historySize > 0) {
                        val pointerCoords: MotionEvent.PointerCoords = MotionEvent.PointerCoords()
                        event.getHistoricalPointerCoords(0, 0, pointerCoords)
                        val dX = event.x - pointerCoords.x
                        val dY = event.y - pointerCoords.y
                        val distance = Math.sqrt(Math.pow(dX.toDouble(), 2.0) + Math.pow(dY.toDouble(), 2.0))
                        topY -= dY
                        leftX -= dX
                        invalidate()
                        isMoving = isMoving || distance > moveTolerance
                        velocityTracker?.addMovement(event)
                    }
                }
                return false
            }
            else -> return super.onTouchEvent(event)
        }
    }

    private fun handleStop(xVelocity: Float, yVelocity: Float) {
        val maxX = (width - nodeSize).toInt() // The right most point is the width of the view less one node
        val maxY = (height - nodeSize).toInt() // The bottom most point is the subHeight of the view less one node
        val minX = ((contentWidth * -1) + nodeSize).toInt() // The left most point is the width of the content less one node
        val minY = ((contentHeight * -1) + nodeSize).toInt() // The top most point is the subHeight of the content less one node
        scroller?.fling(leftX.toInt(), topY.toInt(),
                xVelocity.toInt(), yVelocity.toInt(),
                minX, maxX,
                minY, maxY
                )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    override fun computeScroll() {
        super.computeScroll()
        val localScroller: OverScroller? = scroller
        if (localScroller != null && localScroller.computeScrollOffset()) {
            leftX = localScroller.currX.toFloat()
            topY = localScroller.currY.toFloat()
            ViewCompat.postInvalidateOnAnimation(this)
        }
    }

    private class SavedState: BaseSavedState {
        var tree: Tree? = null
        var leftX: Float = 0f
        var topY: Float = 0f
        var nodeSize: Float = 0f
        var autoSize: Int = -1

        constructor(source: Parcel?) : super(source)

        @RequiresApi(Build.VERSION_CODES.N)
        constructor(source: Parcel?, loader: ClassLoader?) : super(source, loader)

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(out: Parcel?, flags: Int) {
            super.writeToParcel(out, flags)
            out?.writeParcelable(tree, flags)
            out?.writeFloat(leftX)
            out?.writeFloat(topY)
            out?.writeFloat(nodeSize)
            out?.writeInt(autoSize)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel?): SavedState {
                return SavedState(source)
            }

            override fun newArray(size: Int): Array<SavedState> {
                return newArray(size)
            }
        }
    }


    override fun onSaveInstanceState(): Parcelable? {
        val parcelable: Parcelable? = super.onSaveInstanceState()
        val savedState = SavedState(parcelable)
        savedState.tree = tree
        savedState.topY = topY
        savedState.leftX = leftX
        savedState.nodeSize = nodeSize
        savedState.autoSize = if (autoSize) -1 else 0
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        val savedState: SavedState? = state as SavedState
        super.onRestoreInstanceState(savedState?.superState)
        tree = savedState?.tree
        leftX = savedState?.leftX ?: 0f
        topY = savedState?.topY ?: 0f
        nodeSize = savedState?.nodeSize ?: 0f
        autoSize = savedState?.autoSize == -1
    }

    private inner class ScaleListener: ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            nodeSize *= detector?.scaleFactor ?: 1f
            autoSize = false
            if (nodeSize < minNodeSize) {
                nodeSize = minNodeSize
            }
            if (nodeSize > width || nodeSize > height) {
                nodeSize = Math.min(width, height).toFloat()
            }
            postInvalidate()
            return true
        }
    }
}