package com.solderbyte.tessract;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


public class FragmentHelp extends Fragment {

    // Log tag
    private static final String LOG_TAG = "Tessract:FHelp";

    // Fragment Interation Listener
    private FragmentHelp.OnFragmentInteractionListener fragmentInteractionListener;

    public FragmentHelp() {}

    public static FragmentHelp newInstance() {
        Log.d(LOG_TAG, "newInstance");
        FragmentHelp fragment = new FragmentHelp();

        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        Log.d(LOG_TAG, "onAttach");

        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            fragmentInteractionListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreateView");

        // Inflate layout
        View view = inflater.inflate(R.layout.fragment_help, container, false);

        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onFragmentInteraction(this.getString(R.string.title_help));
        }

        // UI listeners

        return view;
    }

    @Override
    public void onDetach() {
        Log.d(LOG_TAG, "onDetach");

        super.onDetach();
        fragmentInteractionListener = null;
    }

    public void onButtonPressed(String title) {
        Log.d(LOG_TAG, "onButtonPressed");

        if (fragmentInteractionListener != null) {
            fragmentInteractionListener.onFragmentInteraction(title);
        }
    }

    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(String title);
    }
}
