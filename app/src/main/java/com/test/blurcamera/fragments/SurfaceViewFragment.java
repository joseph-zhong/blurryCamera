package com.test.blurcamera.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.test.blurcamera.R;
import com.test.blurcamera.utils.BlurEffect;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by sergiosilvajr on 9/20/16.
 */
@SuppressWarnings("deprecation")
public class SurfaceViewFragment extends Fragment implements SurfaceHolder.Callback {
    private SurfaceView cameraSurfaceView;
    private SurfaceHolder surfaceHolder;
    private Camera camera;

    private ImageView imageView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_surface_layout, container, false);
        cameraSurfaceView = (SurfaceView) root.findViewById(R.id.surface_view);
        imageView = (ImageView) root.findViewById(R.id.blur_image_view);

        surfaceHolder = cameraSurfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        releaseResources();
    }

    private void releaseResources() {
        if (camera != null){
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onResume() {
        super.onResume();


    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        camera = Camera.open(1);
        camera.setDisplayOrientation(90);
        try {
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(camera != null){
            Camera.Parameters parameters = camera.getParameters();
            int width = parameters.getPreviewSize().width;
            int height = parameters.getPreviewSize().height;

            camera.addCallbackBuffer(new byte[(int) (width*height*1.5)]);

            camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(final byte[] data, final Camera camera) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                Camera.Parameters parameters = camera.getParameters();
                                int width = parameters.getPreviewSize().width;
                                int height = parameters.getPreviewSize().height;

                                YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                yuv.compressToJpeg(new Rect(0, 0, width, height), 10, out);

                                Matrix matrix = new Matrix();
                                matrix.postRotate(-90);

                                byte[] bytes = out.toByteArray();
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                                bitmap = BlurEffect.apply(getContext(),bitmap);
                                final Bitmap finalBitmap = bitmap;
                                getActivity().runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                imageView.setBackground(new BitmapDrawable(getContext().getResources(), finalBitmap));
                                            }
                                        });
                            }catch(Exception e){
                                return;
                            }
                            camera.addCallbackBuffer(data);

                        }
                    }).start();
                }
            });
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        releaseResources();
    }
}
