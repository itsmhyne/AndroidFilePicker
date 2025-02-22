package com.github.file_picker

import android.Manifest
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.file_picker.adapter.ItemAdapter
import com.github.file_picker.data.model.Media
import com.github.file_picker.data.repository.FilesRepository
import com.github.file_picker.extension.hasPermission
import com.github.file_picker.listener.OnItemClickListener
import com.github.file_picker.listener.OnSubmitClickListener
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ir.one_developer.file_picker.R
import ir.one_developer.file_picker.databinding.FilePickerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.properties.Delegates

/**
 *
 * A fragment that shows a list of items as a modal bottom sheet.
 *
 * You can show this modal bottom sheet from your activity like this:
 * <pre>
 *    FilePicker.Builder([Context]).buildAndShow()
 * </pre>
 */
class FilePicker private constructor(
    builder: Builder
) : BottomSheetDialogFragment() {

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!
    private var _binding: FilePickerBinding? = null

    private var itemsAdapter: ItemAdapter? = null
    private lateinit var repository: FilesRepository

    private var title: String
    private var titleTextColor by Delegates.notNull<Int>()
    private var submitText: String
    private var submitTextColor by Delegates.notNull<Int>()
    private var selectedFiles: List<Media>
    private var fileType: FileType
    private var listDirection: ListDirection
    private var limitCount by Delegates.notNull<Int>()
    private var accentColor by Delegates.notNull<Int>()
    private var gridSpanCount by Delegates.notNull<Int>()
    private var cancellable by Delegates.notNull<Boolean>()
    private var overlayAlpha by Delegates.notNull<Float>()

    private var onItemClickListener: OnItemClickListener?
    private var onSubmitClickListener: OnSubmitClickListener?

    init {
        this.title = builder.title
        this.titleTextColor = builder.titleTextColor
        this.submitText = builder.submitText
        this.submitTextColor = builder.submitTextColor
        this.selectedFiles = builder.selectedFiles
        this.fileType = builder.fileType
        this.listDirection = builder.listDirection
        this.cancellable = builder.cancellable
        this.gridSpanCount = builder.gridSpanCount
        this.limitCount = builder.limitCount
        this.accentColor = builder.accentColor
        this.overlayAlpha = builder.overlayAlpha
        this.onItemClickListener = builder.onItemClickListener
        this.onSubmitClickListener = builder.onSubmitClickListener
    }

    private var requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) loadFiles()
            else dismissAllowingStateLoss()
        }

    class Builder(
        private val appCompatActivity: AppCompatActivity
    ) {
        var title: String = DEFAULT_TITLE
            private set
        var titleTextColor: Int = DEFAULT_TITLE_TEXT_COLOR
            private set
        var submitText: String = DEFAULT_SUBMIT_TEXT
            private set
        var submitTextColor: Int = DEFAULT_SUBMIT_TEXT_COLOR
            private set
        var accentColor: Int = DEFAULT_ACCENT_COLOR
            private set
        var selectedFiles: List<Media> = arrayListOf()
            private set
        var limitCount: Int = DEFAULT_LIMIT_COUNT
            private set
        var fileType: FileType = DEFAULT_FILE_TYPE
            private set
        var gridSpanCount: Int = DEFAULT_SPAN_COUNT
            private set
        var cancellable: Boolean = DEFAULT_CANCELABLE
            private set
        var listDirection: ListDirection = DEFAULT_LIST_DIRECTION
            private set
        var onItemClickListener: OnItemClickListener? = null
            private set
        var onSubmitClickListener: OnSubmitClickListener? = null
            private set
        var overlayAlpha: Float = DEFAULT_OVERLAY_ALPHA
            private set

        /**
         * Set title
         *
         * @param title
         * @return
         */
        fun setTitle(title: String) = apply { this.title = title }

        /**
         * Set title text color
         *
         * @param color
         * @return
         */
        fun setTitleTextColor(color: Int) = apply { this.titleTextColor = color }

        /**
         * Set submit text
         *
         * @param text
         * @return
         */
        fun setSubmitText(text: String) = apply { this.submitText = text }

        /**
         * Set submit text color
         *
         * @param color
         * @return
         */
        fun setSubmitTextColor(color: Int) = apply { this.submitTextColor = color }

        /**
         * Set accent color
         *
         * @param color
         * @return
         */
        fun setAccentColor(@ColorInt color: Int) = apply { this.accentColor = color }

        /**
         * Set selected files, for show as selected style in list
         *
         * @param selectedFiles
         * @return
         */
        fun setSelectedFiles(selectedFiles: List<Media>) =
            apply { this.selectedFiles = selectedFiles }

        /**
         * Set limit item selection
         *
         * @param limit the limit item can select
         * @return
         */
        fun setLimitItemSelection(limit: Int) = apply { this.limitCount = limit }

        /**
         * Set file type
         *
         * @param fileType the [FileType]
         * @return
         */
        fun setFileType(fileType: FileType) = apply { this.fileType = fileType }

        /**
         * Set grid span count
         *
         * @param spanCount the list span count
         * @return
         */
        fun setGridSpanCount(spanCount: Int) = apply { this.gridSpanCount = spanCount }

        /**
         * Set cancellable
         *
         * @param cancellable
         * @return
         */
        fun setCancellable(cancellable: Boolean) = apply { this.cancellable = cancellable }

        /**
         * Set list direction
         *
         * @param listDirection [ListDirection]
         * @return
         */
        fun setListDirection(listDirection: ListDirection) =
            apply { this.listDirection = listDirection }

        /**
         * Set on submit click listener
         *
         * @param onSubmitClickListener
         * @return
         */
        fun setOnSubmitClickListener(
            onSubmitClickListener: OnSubmitClickListener?
        ) = apply { this.onSubmitClickListener = onSubmitClickListener }

        /**
         * Set on item click listener
         *
         * @param onItemClickListener
         * @return
         */
        fun setOnItemClickListener(
            onItemClickListener: OnItemClickListener?
        ) = apply { this.onItemClickListener = onItemClickListener }

        /**
         * Set overlay alpha
         *
         * @param alpha
         */
        fun setOverlayAlpha(
            alpha: Float
        ) = apply { this.overlayAlpha = alpha }

        /**
         * Build file picker instance
         */
        fun build() = FilePicker(this)

        /**
         * Build file picker and show it
         */
        fun buildAndShow() = build().show(
            appCompatActivity.supportFragmentManager,
            "file.picker"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FilePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        repository = FilesRepository(requireActivity().application)
        setCancellableDialog(cancellable)
        setupViews()
    }

    override fun onStart() {
        super.onStart()
        val readStoragePermission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (!hasPermission(readStoragePermission)) {
            requestPermission(readStoragePermission)
            return
        }
        loadFiles()
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (isShown) return
        super.show(manager, tag)
        isShown = true
    }

    override fun onDismiss(dialog: DialogInterface) {
        isShown = false
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        requestPermission.unregister()
    }

    /**
     * Request permission
     *
     * @param permission
     */
    private fun requestPermission(permission: String) = requestPermission.launch(permission)

    /**
     * Set cancellable dialog
     *
     * @param cancellable
     */
    private fun setCancellableDialog(cancellable: Boolean) {
        dialog?.setCancelable(cancellable)
        dialog?.setCanceledOnTouchOutside(cancellable)
    }

    /**
     * Set fixed submit button
     *
     */
    private fun setFixedSubmitButton() {
        val behavior: BottomSheetBehavior<*> = (dialog as BottomSheetDialog).behavior
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {}
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                val button = binding.buttonContainer
                button.y =
                    ((bottomSheet.parent as View).height - bottomSheet.top - button.height).toFloat()
            }
        }.apply {
            val root = binding.root
            root.post { onSlide(root.parent as View, 0f) }
        })
    }

    /**
     * Setup views
     */
    private fun setupViews() = binding.apply {
        changeSubmitButtonState()
        setupRecyclerView(rvFiles)
        setFixedSubmitButton()
        updateSelectedCount()

        cardLine.setCardBackgroundColor(ColorStateList.valueOf(accentColor))
        progress.indeterminateTintList = ColorStateList.valueOf(accentColor)
        tvTitle.apply {
            text = title
            setTextColor(titleTextColor)
        }
        btnSubmit.apply {
            text = submitText
            setOnClickListener {
                submitList()
                dismissAllowingStateLoss()
            }
        }
    }

    /**
     * Show selected count
     *
     */
    private fun updateSelectedCount() {
        val selectedCount = getSelectedItems()?.size ?: 0
        binding.tvTitle.text = "$title ($selectedCount/$limitCount)"
    }

    /**
     * Setup recycler view
     *
     * @param recyclerView
     */
    private fun setupRecyclerView(recyclerView: RecyclerView) {
        itemsAdapter = ItemAdapter(
            accentColor = accentColor,
            overlayAlpha = overlayAlpha,
            limitSelectionCount = limitCount,
            listener = { itemPosition ->
                setupOnItemClickListener(itemPosition)
                updateSelectedCount()
                changeSubmitButtonState()
            }
        )
        recyclerView.apply {
            layoutDirection = when (listDirection) {
                ListDirection.LTR -> RecyclerView.LAYOUT_DIRECTION_LTR
                ListDirection.RTL -> RecyclerView.LAYOUT_DIRECTION_RTL
            }
            layoutManager = GridLayoutManager(requireContext(), gridSpanCount)
            adapter = itemsAdapter
        }
    }

    /**
     * Setup on item click listener
     *
     * @param position
     */
    private fun setupOnItemClickListener(position: Int) {
        if (onItemClickListener == null) return
        if (itemsAdapter == null) return
        val media = itemsAdapter?.snapshot()?.items?.get(position) ?: return
        onItemClickListener?.onClick(media, position, itemsAdapter!!)
    }

    /**
     * change submit button state
     * if has selected item change to enable otherwise disable it
     */
    private fun changeSubmitButtonState() = binding.btnSubmit.apply {
        isEnabled = hasSelectedItem()
        if (isEnabled) {
            setTextColor(submitTextColor)
            setBackgroundColor(accentColor)
            return@apply
        }
        setTextColor(Color.GRAY)
        setBackgroundColor(Color.LTGRAY)
    }

    /**
     * Load files
     */
    private fun loadFiles() = CoroutineScope(Dispatchers.IO).launch {
        itemsAdapter?.addLoadStateListener { state ->
            binding.progress.isVisible = state.source.refresh is LoadState.Loading
            if (state.source.refresh is LoadState.NotLoading) {
                selectedFiles.forEach { media ->
                    itemsAdapter?.snapshot()?.items?.find { it.id == media.id }?.let {
                        it.isSelected = media.isSelected
                        it.order = media.order
                    }
                }
                updateSelectedCount()
                setFixedSubmitButton()
                changeSubmitButtonState()
            }
        }
        repository.getFiles(fileType = fileType).collect { pagingData ->
            itemsAdapter?.submitData(pagingData)
        }
    }

    /**
     * Submit list
     */
    private fun submitList() = getSelectedItems()?.let {
        onSubmitClickListener?.onClick(it)
    }

    /**
     * Get selected items
     *
     * @return
     */
    private fun getSelectedItems(): List<Media>? =
        itemsAdapter?.snapshot()?.items?.filter { it.isSelected }?.sortedBy { it.order }

    /**
     * Has selected item
     *
     * @return
     */
    private fun hasSelectedItem(): Boolean = !getSelectedItems().isNullOrEmpty()

    companion object {
        // Defaults
        const val DEFAULT_SPAN_COUNT = 2
        const val DEFAULT_LIMIT_COUNT = 1
        const val DEFAULT_CANCELABLE = true
        val DEFAULT_FILE_TYPE = FileType.IMAGE
        val DEFAULT_LIST_DIRECTION = ListDirection.LTR

        const val DEFAULT_ACCENT_COLOR = Color.BLACK
        const val DEFAULT_TITLE = "Choose File"
        const val DEFAULT_TITLE_TEXT_COLOR = DEFAULT_ACCENT_COLOR

        @FloatRange(from = 0.0, to = 1.0)
        const val DEFAULT_OVERLAY_ALPHA = 0.5F

        const val DEFAULT_SUBMIT_TEXT = "Submit"
        const val DEFAULT_SUBMIT_TEXT_COLOR = Color.WHITE

        private var isShown: Boolean = false
    }

}