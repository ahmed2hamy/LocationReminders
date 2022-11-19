package com.udacity.project4.base

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R

/**
 * Base Fragment to observe on the common LiveData objects
 */
abstract class BaseFragment : Fragment() {

    /**
     * Every fragment has to have an instance of a view model that extends from the BaseViewModel
     */
    abstract val _viewModel: BaseViewModel

    override fun onStart() {
        super.onStart()

        _viewModel.showErrorMessage.observe(this) {
            showToast(it)
        }

        _viewModel.showToast.observe(this) {
            showToast(it)
        }

        _viewModel.showSnackBar.observe(this) {
            showSnackBar(it)
        }

        _viewModel.showSnackBarInt.observe(this) {
            showSnackBar(getString(it))
        }

        _viewModel.navigationCommand.observe(this) { command ->
            when (command) {
                is NavigationCommand.To -> findNavController().navigate(command.directions)
                is NavigationCommand.Back -> findNavController().popBackStack()
                is NavigationCommand.BackTo -> findNavController().popBackStack(
                    command.destinationId,
                    false
                )
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    fun showSnackBar(
        message: String?,
        duration: Int? = null,
        actionResId: Int? = null,
        action: ((View) -> Unit)? = null
    ) {
        if (message != null) {
            Snackbar.make(requireView(), message, duration ?: Snackbar.LENGTH_LONG)
                .setAction(actionResId ?: R.string.retry, action)
                .show()
        }


    }
}
