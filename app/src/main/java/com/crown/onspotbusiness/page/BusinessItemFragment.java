package com.crown.onspotbusiness.page;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.crown.library.onspotlibrary.controller.OSPreferences;
import com.crown.library.onspotlibrary.model.ListItem;
import com.crown.library.onspotlibrary.model.UnSupportedContent;
import com.crown.library.onspotlibrary.model.business.BusinessOSB;
import com.crown.library.onspotlibrary.model.business.BusinessV6;
import com.crown.library.onspotlibrary.model.businessItem.BusinessItemCard;
import com.crown.library.onspotlibrary.model.businessItem.BusinessItemOSB;
import com.crown.library.onspotlibrary.utils.OSString;
import com.crown.library.onspotlibrary.utils.emun.OSPreferenceKey;
import com.crown.onspotbusiness.R;
import com.crown.onspotbusiness.databinding.FragmentMenuBinding;
import com.crown.onspotbusiness.model.OSBPreferences;
import com.crown.onspotbusiness.view.ListItemAdapter;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BusinessItemFragment extends Fragment implements EventListener<QuerySnapshot> {

    private final String TAG = BusinessItemFragment.class.getName();

    private ListItemAdapter mAdapter;
    private List<ListItem> mDataset;
    private OSBPreferences preferences;
    private FragmentMenuBinding binding;
    private List<DocumentSnapshot> archivedDocs;
    private ListenerRegistration mMenuItemChangeListener;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mDataset = new ArrayList<>();
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);

        binding.warningInclude.warningTv.setText("Create item to see here");
        setHasOptionsMenu(true);
        ((AppCompatActivity) getActivity()).setSupportActionBar(binding.toolbar);
        archivedDocs = new ArrayList<>();
        preferences = OSPreferences.getInstance(getContext()).getObject(OSPreferenceKey.APP_PREFERENCES, OSBPreferences.class);

        binding.listRv.setHasFixedSize(true);
        setUpRecycler();

        return binding.getRoot();
    }

    private void setUpRecycler() {
        if (preferences != null && preferences.getBussItemView() == OSBPreferences.GRID) {
            binding.listRv.setLayoutManager(new GridLayoutManager(getContext(), 2));
        } else {
            binding.listRv.setLayoutManager(new LinearLayoutManager(getContext()));
        }

        mAdapter = new ListItemAdapter(getContext(), mDataset);
        binding.listRv.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        BusinessV6 business = OSPreferences.getInstance(getContext().getApplicationContext()).getObject(OSPreferenceKey.BUSINESS, BusinessV6.class);
        mMenuItemChangeListener = FirebaseFirestore.getInstance().collection(OSString.refItem)
                .whereEqualTo(OSString.fieldBusinessRefId, business.getBusinessRefId())
                .whereEqualTo(OSString.fieldArchived, false)
                .addSnapshotListener(this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_item__primary_options, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_mio_archived_products:
                startActivity(new Intent(getContext(), ArchivedProductsActivity.class));
                break;
            case R.id.nav_mio_add_item:
                Intent intent = new Intent(getContext(), ModifyBusinessItemActivity.class);
                startActivity(intent);
                break;
            case R.id.action_mio_swap:
                if (preferences == null) {
                    preferences = new OSBPreferences();
                    preferences.setBussItemView(OSBPreferences.GRID);
                } else if (preferences.getBussItemView() == OSBPreferences.DETAILS) {
                    preferences.setBussItemView(OSBPreferences.GRID);
                } else {
                    preferences.setBussItemView(OSBPreferences.DETAILS);
                }
                OSPreferences.getInstance(getContext()).setObject(preferences, OSPreferenceKey.APP_PREFERENCES);
                setUpRecycler();
                updateItemList(archivedDocs);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusinessOSB b = OSPreferences.getInstance(getContext()).getObject(OSPreferenceKey.BUSINESS, BusinessOSB.class);
        if (b != null && !b.getIsActive()) {
            binding.inactiveBusinessInclude.inactiveBusinessOib.setVisibility(View.VISIBLE);
            binding.inactiveBusinessInclude.inactiveBusinessOib.setInfoPositiveBtnListener((dialog, which) -> startActivity(new Intent(getContext(), ModifyBusinessActivity.class)));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mMenuItemChangeListener != null) mMenuItemChangeListener.remove();
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot snapshots, @Nullable FirebaseFirestoreException e) {
        Log.v(TAG, "Item changed");
        if (snapshots != null && !snapshots.isEmpty()) {
            updateItemList(snapshots.getDocuments());
            archivedDocs = snapshots.getDocuments();
        } else {
            mDataset.clear();
            mAdapter.notifyDataSetChanged();
            binding.warningInclude.warningTv.setVisibility(View.VISIBLE);
        }
    }

    private void updateItemList(List<DocumentSnapshot> documents) {
        boolean hasUnSupportedItem = false;
        UnSupportedContent unSupportedContent = new UnSupportedContent();
        mDataset.clear();
        for (DocumentSnapshot doc : documents) {
            if (doc.exists()) {
                try {
                    if (preferences != null && preferences.getBussItemView() == OSBPreferences.GRID) {
                        BusinessItemCard item = doc.toObject(BusinessItemCard.class);
                        item.setItemId(doc.getId());
                        mDataset.add(item);
                    } else {
                        BusinessItemOSB item = doc.toObject(BusinessItemOSB.class);
                        item.setItemId(doc.getId());
                        mDataset.add(doc.toObject(BusinessItemOSB.class));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    hasUnSupportedItem = true;
                    unSupportedContent.addItem(doc);
                }
            }
        }
        if (hasUnSupportedItem) mDataset.add(unSupportedContent);
        Collections.sort(mDataset, (o1, o2) -> {
            try {
                return ((BusinessItemOSB) o1).getItemName().compareToIgnoreCase(((BusinessItemOSB) o2).getItemName());
            } catch (Exception ignore) {
            }
            return 0;
        });
        if (mDataset.size() <= 0) binding.warningInclude.warningTv.setVisibility(View.VISIBLE);
        else binding.warningInclude.warningTv.setVisibility(View.GONE);
        mAdapter.notifyDataSetChanged();
    }
}
