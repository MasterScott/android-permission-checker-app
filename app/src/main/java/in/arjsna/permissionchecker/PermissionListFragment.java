package in.arjsna.permissionchecker;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by arjun on 3/6/17.
 */

public class PermissionListFragment extends Fragment {
  private View mRootView;
  private PermissionListAdapter permissionListAdapter;
  private RecyclerView permissionsList;
  private ArrayList<PermissionGroupDetails>  permissionList;

  @Nullable @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    mRootView = inflater.inflate(R.layout.fragment_permission_list, container, false);
    permissionsList = (RecyclerView) mRootView.findViewById(R.id.permission_list);
    permissionListAdapter = new PermissionListAdapter(getContext());
    LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
    permissionsList.setLayoutManager(layoutManager);
    permissionsList.setAdapter(permissionListAdapter);
    if (permissionList == null) {
      makeRx();
    } else {
      permissionListAdapter.addAll(permissionList);
    }
    return mRootView;
  }

  private void makeRx() {
    Single<ArrayList<PermissionGroupDetails>> permissions =
        Single.fromCallable(new Callable<ArrayList<PermissionGroupDetails>>() {
          @Override public ArrayList<PermissionGroupDetails> call() throws Exception {
            Map<String, PermissionGroupDetails> groups = fetchPermList();
            return new ArrayList<>(groups.values());
          }
        });
    permissions.subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new SingleObserver<ArrayList<PermissionGroupDetails>>() {
          @Override public void onSubscribe(@NonNull Disposable d) {

          }

          @Override public void onSuccess(@NonNull ArrayList<PermissionGroupDetails> groupDetailsList) {
            Log.i("Single subscriber test ", groupDetailsList.size() + " ");
            permissionList = groupDetailsList;
            permissionListAdapter.addAll(groupDetailsList);
          }

          @Override public void onError(@NonNull Throwable e) {

          }
        });
  }

  private Map<String, PermissionGroupDetails> fetchPermList() {
    PackageManager packageManager = getActivity().getPackageManager();
    List<PackageInfo> applicationInfos =
        packageManager.getInstalledPackages(PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS);
    Map<String, PermissionGroupDetails> permissionGroupDetailsMap = new HashMap<>();
    PermissionGroupDetails miscPermissionGroup = new PermissionGroupDetails();
    miscPermissionGroup.permissionGroupName = "MISC";
    miscPermissionGroup.permissionGroupName = "Custom/Miscellaneous permissions";
    miscPermissionGroup.appsCount = 0;
    //applicationInfos.clear();
    //try {
    //  applicationInfos.add(packageManager.getPackageInfo("com.indihood.auth", PackageManager.GET_META_DATA | PackageManager.GET_PERMISSIONS));
    //} catch (PackageManager.NameNotFoundException e) {
    //  e.printStackTrace();
    //}
    permissionGroupDetailsMap.put("MISC", miscPermissionGroup);
    for (PackageInfo packageInfo : applicationInfos) {
      try {

        String[] requestedPermissions = packageInfo.requestedPermissions;
        if (requestedPermissions != null) {
          for (String permission : requestedPermissions) {
            PermissionInfo permissionInfo = packageManager.getPermissionInfo(permission, PackageManager.GET_META_DATA);
            if (permissionInfo.group == null) {
              PermissionGroupDetails groupDetails = permissionGroupDetailsMap.get("MISC");
              if (groupDetails.appPackages.add(packageInfo.packageName)) {
                groupDetails.appsCount++;
              }
              continue;
            }
            if (permissionGroupDetailsMap.containsKey(permissionInfo.group)) {
              PermissionGroupDetails groupDetails =
                  permissionGroupDetailsMap.get(permissionInfo.group);
              if (groupDetails.appPackages.add(packageInfo.packageName)) {
                groupDetails.appsCount++;
              }
            } else {
              PermissionGroupDetails permissionGroupDetails = new PermissionGroupDetails();
              permissionGroupDetails.permissionGroupName = permissionInfo.group;
              permissionGroupDetails.permissionGroupDes =
                  permissionInfo.loadDescription(packageManager) == null ? "No desc"
                      : permissionInfo.loadDescription(packageManager).toString();
              if (permissionGroupDetails.appPackages.add(packageInfo.packageName)) {
                permissionGroupDetails.appsCount = 1;
              }
              permissionGroupDetailsMap.put(permissionInfo.group, permissionGroupDetails);
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    Log.i("Map size ", permissionGroupDetailsMap.size() + " ");
    return permissionGroupDetailsMap;
  }
}