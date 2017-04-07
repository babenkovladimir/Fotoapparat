package io.fotoapparat.hardware.v2.captor;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import io.fotoapparat.hardware.CameraException;
import io.fotoapparat.hardware.operators.CaptureOperator;
import io.fotoapparat.hardware.v2.CameraThread;
import io.fotoapparat.hardware.v2.connection.CameraConnection;
import io.fotoapparat.hardware.v2.orientation.OrientationManager;
import io.fotoapparat.hardware.v2.parameters.ParametersManager;
import io.fotoapparat.hardware.v2.session.SessionManager;
import io.fotoapparat.parameter.FocusMode;
import io.fotoapparat.parameter.Parameters;
import io.fotoapparat.photo.Photo;

import static io.fotoapparat.hardware.v2.capabilities.FocusCapability.focusToAfMode;
import static io.fotoapparat.parameter.Parameters.Type.FOCUS_MODE;

/**
 * Responsible to capture a picture.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PictureCaptor extends CameraCaptureSession.CaptureCallback implements CaptureOperator {

	private final SurfaceReader surfaceReader;
	private final CameraConnection cameraConnection;
	private final SessionManager sessionManager;
	private final ParametersManager parametersManager;
	private final OrientationManager orientationManager;

	public PictureCaptor(SurfaceReader surfaceReader,
						 CameraConnection cameraConnection,
						 SessionManager sessionManager,
						 ParametersManager parametersManager,
						 OrientationManager orientationManager) {
		this.surfaceReader = surfaceReader;
		this.cameraConnection = cameraConnection;
		this.sessionManager = sessionManager;
		this.parametersManager = parametersManager;
		this.orientationManager = orientationManager;
	}

	private static int getFocusMode(Parameters parameters) {
		FocusMode focusMode = parameters.getValue(FOCUS_MODE);
		return focusToAfMode(focusMode);
	}

	@Override
	public void onCaptureCompleted(@NonNull CameraCaptureSession session,
								   @NonNull CaptureRequest request,
								   @NonNull TotalCaptureResult result) {
		super.onCaptureCompleted(session, request, result);
	}

	@Override
	public void onCaptureFailed(@NonNull CameraCaptureSession session,
								@NonNull CaptureRequest request,
								@NonNull CaptureFailure failure) {
		super.onCaptureFailed(session, request, failure);
		// TODO: 27.03.17 support failure
	}

	/**
	 * Captures photo synchronously.
	 *
	 * @return a new Photo
	 */
	@Override
	public Photo takePicture() {
		CameraCaptureSession captureSession = sessionManager.getCaptureSession();
		Integer sensorOrientation = orientationManager.getSensorOrientation();
		try {
			capture(captureSession, sensorOrientation);
		} catch (CameraAccessException e) {
			throw new CameraException(e);
		}

		return new Photo(
				surfaceReader.getPhotoBytes(),
				sensorOrientation
		);
	}

	private void capture(CameraCaptureSession session,
						 Integer sensorOrientation) throws CameraAccessException {
		//		session.stopRepeating(); // TODO: 05.04.17 need?
		session.capture(
				createCaptureRequest(sensorOrientation),
				this,
				CameraThread
						.getInstance()
						.createHandler()
		);
	}

	private CaptureRequest createCaptureRequest(Integer sensorOrientation) throws
			CameraAccessException {
		CaptureRequest.Builder requestBuilder = cameraConnection
				.getCamera()
				.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);


		Parameters parameters = parametersManager.getParameters();

		requestBuilder.addTarget(surfaceReader.getSurface());
		requestBuilder.set(CaptureRequest.JPEG_ORIENTATION, sensorOrientation);

		requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, getFocusMode(parameters));

		return requestBuilder.build();
	}

}