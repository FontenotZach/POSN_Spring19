package com.posn.main.wall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.posn.Constants;
import com.posn.R;
import com.posn.asynctasks.GetFriendContentAsyncTask;
import com.posn.asynctasks.wall.NewWallPhotoPostAsyncTask;
import com.posn.asynctasks.wall.NewWallStatusPostAsyncTask;
import com.posn.datatypes.Friend;
import com.posn.datatypes.WallPost;
import com.posn.main.MainActivity;
import com.posn.main.wall.comments.CommentActivity;
import com.posn.main.wall.posts.ListViewPostItem;
import com.posn.main.wall.posts.PhotoPostItem;
import com.posn.main.wall.posts.StatusPostItem;
import com.posn.main.wall.posts.VideoPostItem;
import com.posn.main.AppDataManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This class implements the functionality for the user wall fragment:
 * <ul><li>Populates the list view using the data stored in the wall post hashmap located in the data manager class in the main activity
 * <li>Allows the comments for each post to be view (launches a new activity for comments)
 * <li>Allows the user to create new status or photo wall posts</ul>
 **/

public class UserWallFragment extends Fragment implements OnClickListener, OnRefreshListener
   {
      // user interface variables
      RelativeLayout statusButton, photoButton;
      public TextView noWallPostsText;
      ListView lv;
      TableRow statusBar;
      SwipeRefreshLayout swipeLayout;


      public ArrayList<ListViewPostItem> listViewItems = new ArrayList<>();
      HashMap<String, WallPost> wallPostData;
      public WallArrayAdapter adapter;

      public MainActivity main;
      public AppDataManager dataManager;


      @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
         {
            super.onCreate(savedInstanceState);

            // load the wall fragment layout
            View view = inflater.inflate(R.layout.fragment_user_wall, container, false);

            // get the main activity
            main = (MainActivity) getActivity();
            dataManager = main.dataManager;

            // get the listview from the layout
            lv = (ListView) view.findViewById(R.id.listView1);
            noWallPostsText = (TextView) view.findViewById(R.id.notification_text);

            // get the buttons from the layout
            statusButton = (RelativeLayout) view.findViewById(R.id.status_button);
            photoButton = (RelativeLayout) view.findViewById(R.id.photo_button);

            // set an onclick listener for each button
            statusButton.setOnClickListener(this);
            photoButton.setOnClickListener(this);

            // get the bottom status bar  from the layout
            statusBar = (TableRow) view.findViewById(R.id.status_bar);

            swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_container);
            swipeLayout.setOnRefreshListener(this);


            //
            lv.setOnScrollListener(new OnScrollListener()
               {
                  @Override
                  public void onScrollStateChanged(AbsListView view, int scrollState)
                     {
                     }

                  @Override
                  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
                     {
                        int topRowVerticalPosition = (lv == null || lv.getChildCount() == 0) ? 0 : lv.getChildAt(0).getTop();
                        swipeLayout.setEnabled(firstVisibleItem == 0 && topRowVerticalPosition >= 0);
                     }
               });

            adapter = new WallArrayAdapter(getActivity(), listViewItems);
            lv.setAdapter(adapter);

            // get the wall post data from activity
            wallPostData = dataManager.masterWallPostList.wallPosts;
            if (wallPostData.size() != 0)
               {
                  updateWallPosts();
               }

            return view;
         }

      @Override
      public void onClick(View v)
         {
            WallPost wallPost;
            Intent intent;

            switch (v.getId())
               {
                  case R.id.status_button:
                     intent = new Intent(main, CreateNewStatusPostActivity.class);
                     intent.putExtra("groups", dataManager.userGroupList.getUserGroupsArrayList());
                     startActivityForResult(intent, Constants.RESULT_CREATE_STATUS_POST);
                     break;

                  case R.id.photo_button:
                     intent = new Intent(main, CreateNewPhotoPostActivity.class);
                     intent.putExtra("groups", dataManager.userGroupList.getUserGroupsArrayList());
                     startActivityForResult(intent, Constants.RESULT_CREATE_PHOTO_POST);
                     break;



                  // buttons on individual posts
                  case R.id.comment_button:
                     wallPost = (WallPost) v.getTag();
                     intent = new Intent(main, CommentActivity.class);
                     intent.putExtra("post", wallPost);
                     intent.putExtra("friends", dataManager.masterFriendList.currentFriends);
                     intent.putExtra("user", dataManager.user);
                     main.startActivity(intent);
                     break;

                  case R.id.photo:
                     wallPost = (WallPost) v.getTag();
                     intent = new Intent(main, PhotoViewerActivity.class);
                     intent.putExtra("post", wallPost);
                     main.startActivity(intent);
                     break;

                  case R.id.video:
                     wallPost = (WallPost) v.getTag();
                     intent = new Intent(main, VideoPlayerActivity.class);
                     intent.putExtra("post", wallPost);
                     main.startActivity(intent);
                     break;
               }
         }


      @Override
      public void onActivityResult(int requestCode, int resultCode, Intent data)
         {
            // check if the created post was a status text
            if (requestCode == Constants.RESULT_CREATE_STATUS_POST && resultCode == Activity.RESULT_OK)
               {
                  String status = data.getStringExtra("status");
                  ArrayList<String> selectGroups = data.getStringArrayListExtra("groups");
                  new NewWallStatusPostAsyncTask(this, selectGroups, status).execute();
               }
            // check if the created post was a photo
            else if (requestCode == Constants.RESULT_CREATE_PHOTO_POST && resultCode == Activity.RESULT_OK)
               {
                  String photopath = data.getStringExtra("photopath");
                  ArrayList<String> selectGroups = data.getStringArrayListExtra("groups");
                  new NewWallPhotoPostAsyncTask(this, selectGroups, photopath).execute();
               }
         }

      public void createWallPostsList()
         {
            // declared variables
            String name;
            WallPost wallPost;

            // clear the list view
            listViewItems.clear();

            System.out.println("CREATING WALL POSTS!!!");

            // loop through all the wall posts and add them to the listview
            for (Map.Entry<String, WallPost> entry : wallPostData.entrySet())
               {
                  // get the post from the hashmap
                  wallPost = entry.getValue();

                  // get the name of the person who created the post
                  if (wallPost.friendID.equals(dataManager.user.ID))
                     {
                        name = dataManager.user.firstName + " " + dataManager.user.lastName;
                     }
                  else
                     {
                        Friend friend = dataManager.masterFriendList.currentFriends.get(wallPost.friendID);
                        name = friend.name;
                     }

                  // check if the post is an image
                  if (wallPost.type == Constants.POST_TYPE_PHOTO)
                     {
                        String photoPath = Constants.multimediaFilePath + "/" + wallPost.postID + ".jpg";
                        File imgFile = new File(photoPath);
                        if (imgFile.exists())
                           {
                              listViewItems.add(new PhotoPostItem(this, name, wallPost));
                           }
                     }
                  // check if the post is a link or status
                  else if (wallPost.type == Constants.POST_TYPE_STATUS || wallPost.type == Constants.POST_TYPE_LINK)
                     {
                        listViewItems.add(new StatusPostItem(this, name, wallPost));
                     }
                  // check if the post is a video
                  else if (wallPost.type == Constants.POST_TYPE_VIDEO)
                     {
                        listViewItems.add(new VideoPostItem(this, name, wallPost));
                     }
               }
            sortWallPostList();
         }


      @Override
      public void onRefresh()
         {
            new Handler().post(new Runnable()
               {
                  @Override
                  public void run()
                     {
                        swipeLayout.setRefreshing(false);
                        Toast.makeText(getActivity(), "Refreshing...", Toast.LENGTH_SHORT).show();
                        new GetFriendContentAsyncTask(main).execute();

                     }
               });
         }

      public void sortWallPostList()
         {
            Comparator<ListViewPostItem> postDateComparator = new Comparator<ListViewPostItem>()
               {
                  public int compare(ListViewPostItem emp1, ListViewPostItem emp2)
                     {
                        return (emp1.getDate().compareTo(emp2.getDate()) * -1);
                     }
               };

            Collections.sort(listViewItems, postDateComparator);
         }

      public void updateWallPosts()
         {
            createWallPostsList();

            if (wallPostData.size() > 0)
               {
                  noWallPostsText.setVisibility(View.GONE);
               }
            else
               {
                  noWallPostsText.setVisibility(View.VISIBLE);
               }

            // notify the adapter about the data change
            adapter.notifyDataSetChanged();
         }


   }
