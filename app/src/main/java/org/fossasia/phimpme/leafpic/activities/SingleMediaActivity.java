package org.fossasia.phimpme.leafpic.activities;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.yalantis.ucrop.UCrop;

import org.fossasia.phimpme.R;
import org.fossasia.phimpme.SharingActivity;
import org.fossasia.phimpme.base.SharedMediaActivity;
import org.fossasia.phimpme.editor.FileUtils;
import org.fossasia.phimpme.editor.editimage.EditImageActivity;
import org.fossasia.phimpme.editor.editimage.utils.BitmapUtils;
import org.fossasia.phimpme.leafpic.SelectAlbumBottomSheet;
import org.fossasia.phimpme.leafpic.adapters.MediaPagerAdapter;
import org.fossasia.phimpme.leafpic.animations.DepthPageTransformer;
import org.fossasia.phimpme.leafpic.data.Album;
import org.fossasia.phimpme.leafpic.util.AlertDialogsHelper;
import org.fossasia.phimpme.leafpic.util.ColorPalette;
import org.fossasia.phimpme.leafpic.util.ContentHelper;
import org.fossasia.phimpme.leafpic.util.Measure;
import org.fossasia.phimpme.leafpic.util.PreferenceUtil;
import org.fossasia.phimpme.leafpic.util.SecurityHelper;
import org.fossasia.phimpme.leafpic.util.StringUtils;
import org.fossasia.phimpme.leafpic.views.HackyViewPager;
import org.fossasia.phimpme.utilities.ActivitySwitchHelper;

import java.io.File;

/**
 * Created by dnld on 18/02/16.
 */
@SuppressWarnings("ResourceAsColor")
public class SingleMediaActivity extends SharedMediaActivity {

    private static final String ISLOCKED_ARG = "isLocked";
    static final String ACTION_OPEN_ALBUM = "android.intent.action.pagerAlbumMedia";
    private static final String ACTION_REVIEW = "com.android.camera.action.REVIEW";

    private HackyViewPager mViewPager;
    private MediaPagerAdapter adapter;
    private PreferenceUtil SP;
    private RelativeLayout ActivityBackground;
    private SelectAlbumBottomSheet bottomSheetDialogFragment;
    private SecurityHelper securityObj;
    private Toolbar toolbar;
    private boolean fullScreenMode, customUri = false;
    public static final int REQUEST_PERMISSON_SORAGE = 1;
    public static final int REQUEST_PERMISSON_CAMERA = 2;
    public static final int SELECT_GALLERY_IMAGE_CODE = 7;
    public static final int TAKE_PHOTO_CODE = 8;
    public static final int ACTION_REQUEST_EDITIMAGE = 9;
    public static final int ACTION_STICKERS_IMAGE = 10;
    private ImageView imgView;
    private View openAblum;
    private View editImage;//
    private Bitmap mainBitmap;
    private int imageWidth, imageHeight;//
    private String path;
    private SingleMediaActivity context;
    public static final String EXTRA_OUTPUT = "extra_output";
    private String pathForDescriptionActivity;


    private View mTakenPhoto;//拍摄照片用于编辑
    private Uri photoURI = null;

    //To handle all photos
    public Boolean allPhotoMode;
    public int all_photo_pos;
    public int size_all;
    public int current_image_pos;
    private Uri uri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);
        initView();

        SP = PreferenceUtil.getInstance(getApplicationContext());
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        mViewPager = (HackyViewPager) findViewById(R.id.photos_pager);
        securityObj= new SecurityHelper(SingleMediaActivity.this);
        allPhotoMode = getIntent().getBooleanExtra(getString(R.string.all_photo_mode), false);
        all_photo_pos = getIntent().getIntExtra(getString(R.string.position), 0);
        size_all = getIntent().getIntExtra(getString(R.string.allMediaSize), getAlbum().getCount());

        //For description activity only
        String path2 = getIntent().getStringExtra("path");
        this.pathForDescriptionActivity=path2;
        //

        if (savedInstanceState != null)
            mViewPager.setLocked(savedInstanceState.getBoolean(ISLOCKED_ARG, false));
        try
        {
            Album album;
            if ((getIntent().getAction().equals(Intent.ACTION_VIEW) || getIntent().getAction().equals(ACTION_REVIEW)) && getIntent().getData() != null) {

                File file = null;
                if (path != null)
                    file = new File(path);

                if (file != null && file.isFile())
                    //the image is stored in the storage
                    album = new Album(getApplicationContext(), file);
                else {
                    //try to show with Uri
                    album = new Album(getApplicationContext(), getIntent().getData());
                    customUri = true;
                }
                getAlbums().addAlbum(0, album);
            }
            initUI();
            setupUI();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void initView() {
        context = this;
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        imageWidth = metrics.widthPixels;
        imageHeight = metrics.heightPixels;

        imgView = (ImageView) findViewById(R.id.img);
    }

    private void initUI() {

        setSupportActionBar(toolbar);
        toolbar.bringToFront();
        toolbar.setNavigationIcon(getToolbarIcon(CommunityMaterial.Icon.cmd_arrow_left));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        setRecentApp(getString(R.string.app_name));
        setupSystemUI();

        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) showSystemUI();
                        else hideSystemUI();
                    }
                });


        if (!allPhotoMode) {
            adapter = new MediaPagerAdapter(getSupportFragmentManager(), getAlbum().getMedia());

            getSupportActionBar().setTitle((getAlbum().getCurrentMediaIndex() + 1) + " " + getString(R.string.of) + " " + getAlbum().getMedia().size());
            mViewPager.setAdapter(adapter);
            mViewPager.setCurrentItem(getAlbum().getCurrentMediaIndex());
            mViewPager.setPageTransformer(true, new DepthPageTransformer());
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    getAlbum().setCurrentPhotoIndex(position);
                    toolbar.setTitle((position + 1) + " " + getString(R.string.of) + " " + getAlbum().getMedia().size());
                    invalidateOptionsMenu();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        } else {
            adapter = new MediaPagerAdapter(getSupportFragmentManager(), LFMainActivity.listAll);
            getSupportActionBar().setTitle(all_photo_pos + 1 + " " + getString(R.string.of) + " " + size_all);
            current_image_pos = all_photo_pos;
            mViewPager.setAdapter(adapter);
            mViewPager.setCurrentItem(all_photo_pos);
            mViewPager.setPageTransformer(true, new DepthPageTransformer());
            mViewPager.setOffscreenPageLimit(3);
            mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                }

                @Override
                public void onPageSelected(int position) {
                    current_image_pos = position;
                    getAlbum().setCurrentPhotoIndex(position);
                    toolbar.setTitle((position + 1) + " " + getString(R.string.of) + " " + size_all);
                    invalidateOptionsMenu();
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                }
            });
        }
        Display aa = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();

        if (aa.getRotation() == Surface.ROTATION_90) {
            Configuration configuration = new Configuration();
            configuration.orientation = Configuration.ORIENTATION_LANDSCAPE;
            onConfigurationChanged(configuration);

        }

    }

    private void setupUI() {

        /**** Theme ****/
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(
                isApplyThemeOnImgAct()
                        ? ColorPalette.getTransparentColor (getPrimaryColor(), getTransparency())
                        : ColorPalette.getTransparentColor(getDefaultThemeToolbarColor3th(), 175));

        toolbar.setPopupTheme(getPopupToolbarStyle());

        ActivityBackground = (RelativeLayout) findViewById(R.id.PhotoPager_Layout);
        ActivityBackground.setBackgroundColor(getBackgroundColor());

        setStatusBarColor();
        setNavBarColor();

        securityObj.updateSecuritySetting();

        /**** SETTINGS ****/

        if (SP.getBoolean("set_max_luminosity", false))
            updateBrightness(1.0F);
        else try {
            float brightness = Settings.System.getInt(
                    getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            brightness = brightness == 1.0F ? 255.0F : brightness;
            updateBrightness(brightness);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }

        if (SP.getBoolean("set_picture_orientation", false))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        else setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }


    @Override
    public void onResume() {
        super.onResume();
        ActivitySwitchHelper.setContext(this);
        setupUI();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(getApplicationContext()).clearMemory();
        Glide.get(getApplicationContext()).trimMemory(TRIM_MEMORY_COMPLETE);
        System.gc();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_view_pager, menu);

            menu.findItem(R.id.action_delete).setIcon(getToolbarIcon(CommunityMaterial.Icon.cmd_delete));
            menu.findItem(R.id.action_share).setIcon(getToolbarIcon(GoogleMaterial.Icon.gmd_share));

        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
            params.setMargins(0,0,Measure.getNavigationBarSize(SingleMediaActivity.this).x,0);
        else
            params.setMargins(0,0,0,0);

        toolbar.setLayoutParams(params);
    }

    @Override
    public boolean onPrepareOptionsMenu(final Menu menu) {

        if (!allPhotoMode)
            menu.setGroupVisible(R.id.only_photos_options, !getAlbum().getCurrentMedia().isVideo());

        if (customUri) {
            menu.setGroupVisible(R.id.on_internal_storage, false);
            menu.setGroupVisible(R.id.only_photos_options, false);
            menu.findItem(R.id.sort_action).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data != null && resultCode == RESULT_OK) {
            switch (requestCode) {
                case UCrop.REQUEST_CROP:
                    final Uri imageUri = UCrop.getOutput(data);
                    if (imageUri != null && imageUri.getScheme().equals("file")) {
                        try {
                            //copyFileToDownloads(imageUri);
                            // TODO: 21/08/16 handle this better
                            handleEditorImage(data);
                            if(ContentHelper.copyFile(getApplicationContext(), new File(imageUri.getPath()), new File(getAlbum().getPath()))) {
                                //((ImageFragment) adapter.getRegisteredFragment(getAlbum().getCurrentMediaIndex())).displayMedia(true);
                                Toast.makeText(this, R.string.new_file_created, Toast.LENGTH_SHORT).show();
                            }
                            //adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            Log.e("ERROS - uCrop", imageUri.toString(), e);
                        }
                    } else
                        StringUtils.showToast(getApplicationContext(), "errori random");
                    break;
                default:
                    break;
            }
        }
    }

    private void handleEditorImage(Intent data) {
        String newFilePath = data.getStringExtra(EditImageActivity.EXTRA_OUTPUT);
        boolean isImageEdit = data.getBooleanExtra(EditImageActivity.IMAGE_IS_EDIT, false);

        if (isImageEdit){

        }else{//未编辑  还是用原来的图片
            newFilePath = data.getStringExtra(EditImageActivity.FILE_PATH);;
        }
        //System.out.println("newFilePath---->" + newFilePath);
        //File file = new File(newFilePath);
        //System.out.println("newFilePath size ---->" + (file.length() / 1024)+"KB");
        Log.d("image is edit", isImageEdit + "");
        LoadImageTask loadTask = new LoadImageTask();
        loadTask.execute(newFilePath);
    }

    private void startLoadTask() {
        LoadImageTask task = new LoadImageTask();
        task.execute(path);
    }


    private void displayAlbums(boolean reload) {
        Intent i = new Intent(SingleMediaActivity.this, LFMainActivity.class);
        Bundle b = new Bundle();
        b.putInt(SplashScreen.CONTENT, SplashScreen.ALBUMS_PREFETCHED);
        if (!reload) i.putExtras(b);
        startActivity(i);
        finish();
    }

    private void deleteCurrentMedia() {
        if (!allPhotoMode) {
            getAlbum().deleteCurrentMedia(getApplicationContext());
            if (getAlbum().getMedia().size() == 0) {
                if (customUri) finish();
                else {
                    getAlbums().removeCurrentAlbum();
                    displayAlbums(false);
                }
            }
            adapter.notifyDataSetChanged();
            toolbar.setTitle((mViewPager.getCurrentItem() + 1) + " " + getString(R.string.of) + " " + getAlbum().getMedia().size());
        } else {
            deleteMedia(LFMainActivity.listAll.get(current_image_pos).getPath());
            LFMainActivity.listAll.remove(current_image_pos);
            size_all = LFMainActivity.listAll.size();
            adapter.notifyDataSetChanged();
            mViewPager.setCurrentItem(current_image_pos);
            toolbar.setTitle((mViewPager.getCurrentItem() + 1) + " " + getString(R.string.of) + " " + size_all);
        }
    }

    private void deleteMedia(String path) {
        String[] projection = {MediaStore.Images.Media._ID};

        // Match on the file path
        String selection = MediaStore.Images.Media.DATA + " = ?";
        String[] selectionArgs = new String[]{path};

        // Query for the ID of the media matching the file path
        Uri queryUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver contentResolver = getContentResolver();
        Cursor c = contentResolver.query(queryUri, projection, selection, selectionArgs, null);
        if (c.moveToFirst()) {
            // We found the ID. Deleting the item via the content provider will also remove the file
            long id = c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            Uri deleteUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
            contentResolver.delete(deleteUri, null, null);
        }
        c.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_copy:
                bottomSheetDialogFragment = new SelectAlbumBottomSheet();
                bottomSheetDialogFragment.setTitle(getString(R.string.copy_to));
                bottomSheetDialogFragment.setSelectAlbumInterface(new SelectAlbumBottomSheet.SelectAlbumInterface() {
                    @Override
                    public void folderSelected(String path) {
                        getAlbum().copyPhoto(getApplicationContext(), getAlbum().getCurrentMedia().getPath(), path);
                        bottomSheetDialogFragment.dismiss();
                    }
                });
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
                break;

            case R.id.action_share:
                Intent share = new Intent(SingleMediaActivity.this, SharingActivity.class);
                if (!allPhotoMode)
                    share.putExtra(EXTRA_OUTPUT, getAlbum().getCurrentMedia().getPath());
                else
                    share.putExtra(EXTRA_OUTPUT, LFMainActivity.listAll.get(current_image_pos).getPath());
                startActivity(share);
                return true;

            case R.id.action_edit:
                File outputFile = FileUtils.genEditFile();
                if (!allPhotoMode)
                    uri = Uri.fromFile(new File(getAlbum().getCurrentMedia().getPath()));
                else
                    uri = Uri.fromFile(new File(LFMainActivity.listAll.get(current_image_pos).getPath()));
                EditImageActivity.start(this,uri.getPath(),outputFile.getAbsolutePath(),ACTION_REQUEST_EDITIMAGE);
                break;

            case R.id.action_use_as:
                Intent intent = new Intent(Intent.ACTION_ATTACH_DATA);
                if (!allPhotoMode)
                    intent.setDataAndType(
                            getAlbum().getCurrentMedia().getUri(), getAlbum().getCurrentMedia().getMimeType());
                else
                    intent.setDataAndType(Uri.fromFile(new File(LFMainActivity.listAll.get(current_image_pos).getPath())), StringUtils.getMimeType(LFMainActivity.listAll.get(current_image_pos).getPath()));
                startActivity(Intent.createChooser(intent, getString(R.string.use_as)));
                return true;

            case R.id.action_delete:
                final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(SingleMediaActivity.this, getDialogStyle());

                AlertDialogsHelper.getTextDialog(SingleMediaActivity.this,deleteDialog,
                        R.string.delete, R.string.delete_photo_message);

                deleteDialog.setNegativeButton(this.getString(R.string.cancel).toUpperCase(), null);
                deleteDialog.setPositiveButton(this.getString(R.string.delete).toUpperCase(), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if (securityObj.isActiveSecurity()&&securityObj.isPasswordOnDelete()) {

                            final AlertDialog.Builder passwordDialogBuilder = new AlertDialog.Builder(SingleMediaActivity.this, getDialogStyle());
                            final EditText editTextPassword = securityObj.getInsertPasswordDialog
                                    (SingleMediaActivity.this, passwordDialogBuilder);

                            passwordDialogBuilder.setPositiveButton(getString(R.string.ok_action).toUpperCase(), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    if (securityObj.checkPassword(editTextPassword.getText().toString())) {
                                        deleteCurrentMedia();
                                    } else
                                        Toast.makeText(passwordDialogBuilder.getContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();

                                }
                            });
                            passwordDialogBuilder.setNegativeButton(getString(R.string.cancel).toUpperCase(), null);
                            final AlertDialog passwordDialog = passwordDialogBuilder.create();
                            passwordDialog.show();
                            passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View
                                    .OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    if (securityObj.checkPassword(editTextPassword.getText().toString())){
                                        deleteCurrentMedia();
                                        passwordDialog.dismiss();
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.wrong_password, Toast.LENGTH_SHORT).show();
                                        editTextPassword.getText().clear();
                                        editTextPassword.requestFocus();
                                    }
                                }
                            });
                        } else
                            deleteCurrentMedia();
                    }
                });
                deleteDialog.show();
                return true;

            case R.id.action_move:
                bottomSheetDialogFragment = new SelectAlbumBottomSheet();
                bottomSheetDialogFragment.setTitle(getString(R.string.move_to));
                bottomSheetDialogFragment.setSelectAlbumInterface(new SelectAlbumBottomSheet.SelectAlbumInterface() {
                    @Override
                    public void folderSelected(String path) {
                        getAlbum().moveCurrentMedia(getApplicationContext(), path);

                        if (getAlbum().getMedia().size() == 0) {
                            if (customUri) finish();
                            else {
                                getAlbums().removeCurrentAlbum();
                                //((MyApplication) getApplicationContext()).removeCurrentAlbum();
                                displayAlbums(false);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        toolbar.setTitle((mViewPager.getCurrentItem() + 1) + " " + getString(R.string.of) + " " + getAlbum().getCount());
                        bottomSheetDialogFragment.dismiss();
                    }
                });
                bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());

                return true;

            case R.id.action_details:
                AlertDialog.Builder detailsDialogBuilder = new AlertDialog.Builder(SingleMediaActivity.this, getDialogStyle());
                AlertDialog detailsDialog;
                if (allPhotoMode)
                    detailsDialog =
                            AlertDialogsHelper.getDetailsDialog(this, detailsDialogBuilder, LFMainActivity.listAll.get(current_image_pos));
                else
                    detailsDialog =
                            AlertDialogsHelper.getDetailsDialog(this, detailsDialogBuilder, getAlbum().getCurrentMedia());

                detailsDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string
                        .ok_action).toUpperCase(), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { }});
                detailsDialog.show();
                break;

            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;

            case R.id.action_description:
                Intent intent1 = new Intent(this, DescriptionActivity.class);
                intent1.putExtra("path", pathForDescriptionActivity);
                startActivity(intent1);
                break;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                //return super.onOptionsItemSelected(item);
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateBrightness(float level) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = level;
        getWindow().setAttributes(lp);
    }

    @SuppressWarnings("ResourceAsColor")
    private UCrop.Options getUcropOptions() {

        UCrop.Options options = new UCrop.Options();
        options.setCompressionFormat(Bitmap.CompressFormat.PNG);
        options.setCompressionQuality(90);
        options.setActiveWidgetColor(getAccentColor());
        options.setToolbarColor(getPrimaryColor());
        options.setStatusBarColor(isTranslucentStatusBar() ? ColorPalette.getObscuredColor(getPrimaryColor()) : getPrimaryColor());
        options.setCropFrameColor(getAccentColor());
        options.setFreeStyleCropEnabled(true);

        return options;
    }

    @Override
    public void setNavBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isApplyThemeOnImgAct())
                if (isNavigationBarColored())
                    getWindow().setNavigationBarColor(ColorPalette.getTransparentColor(getPrimaryColor(), getTransparency()));
                else
                    getWindow().setNavigationBarColor(ColorPalette.getTransparentColor(ContextCompat.getColor(getApplicationContext(), R.color.md_black_1000), getTransparency()));
            else
                getWindow().setNavigationBarColor(ColorPalette.getTransparentColor(ContextCompat.getColor(getApplicationContext(), R.color.md_black_1000), 175));
        }
    }

    @Override
    protected void setStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (isApplyThemeOnImgAct())
                if (isTranslucentStatusBar() && isTransparencyZero())
                    getWindow().setStatusBarColor(ColorPalette.getObscuredColor(getPrimaryColor()));
                else
                    getWindow().setStatusBarColor(ColorPalette.getTransparentColor(getPrimaryColor(), getTransparency()));
            else
                getWindow().setStatusBarColor(ColorPalette.getTransparentColor(
                        ContextCompat.getColor(getApplicationContext(), R.color.md_black_1000), 175));
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        if (mViewPager != null) {
            outState.putBoolean(ISLOCKED_ARG, mViewPager.isLocked());
        }
        super.onSaveInstanceState(outState);
    }

    public void toggleSystemUI() {
        if (fullScreenMode)
            showSystemUI();
        else hideSystemUI();
    }

    private void hideSystemUI() {
        runOnUiThread(new Runnable() {
            public void run() {
                toolbar.animate().translationY(-toolbar.getHeight()).setInterpolator(new AccelerateInterpolator())
                        .setDuration(200).start();
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                | View.SYSTEM_UI_FLAG_IMMERSIVE);

                fullScreenMode = true;
                changeBackGroundColor();
            }
        });
    }

    private void setupSystemUI() {
        toolbar.animate().translationY(Measure.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                .setDuration(0).start();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private void showSystemUI() {
        runOnUiThread(new Runnable() {
            public void run() {
                toolbar.animate().translationY(Measure.getStatusBarHeight(getResources())).setInterpolator(new DecelerateInterpolator())
                        .setDuration(240).start();
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
                fullScreenMode = false;
                changeBackGroundColor();
            }
        });
    }

    private void changeBackGroundColor() {
        int colorTo;
        int colorFrom;
        if (fullScreenMode) {
            colorFrom = getBackgroundColor();
            colorTo = (ContextCompat.getColor(SingleMediaActivity.this, R.color.md_black_1000));
        } else {
            colorFrom = (ContextCompat.getColor(SingleMediaActivity.this, R.color.md_black_1000));
            colorTo = getBackgroundColor();
        }
        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.setDuration(240);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                ActivityBackground.setBackgroundColor((Integer) animator.getAnimatedValue());
            }
        });
        colorAnimation.start();
    }
    private final class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
        @Override
        protected Bitmap doInBackground(String... params) {
            return BitmapUtils.getSampledBitmap(params[0], imageWidth / 4, imageHeight / 4);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        protected void onCancelled(Bitmap result) {
            super.onCancelled(result);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            if (mainBitmap != null) {
                mainBitmap.recycle();
                mainBitmap = null;
                System.gc();
            }
            mainBitmap = result;
            imgView.setImageBitmap(mainBitmap);
        }
    }
}
