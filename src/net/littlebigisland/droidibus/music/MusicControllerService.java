package net.littlebigisland.droidibus.music;
 
/**
 * MusicControllerService class
 * Handle communication between active Media Sessions and our Activity
 * by implementing Android "L" MusicControllerService class
 * @author Ted <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.session.MediaSession.Token;
import android.media.session.MediaSessionManager;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.util.Log;
import android.view.KeyEvent;


public class MusicControllerService extends NotificationListenerService implements MediaSessionManager.OnActiveSessionsChangedListener{
    
    private static String TAG = "DroidIBus";
    private static String CTAG = "MediaControllerService: ";
    private Context mContext;
    private IBinder mBinder = new MusicControllerBinder();
    
    Map<String, MediaController> mMediaControllers = new HashMap<String, MediaController>();
    private MediaController mActiveMediaController = null;
    private MediaController.TransportControls mActiveMediaTransport = null;
    
    Map<String, String> mMediaControllerSessionMap = new HashMap<String, String>();
    
    // Callback provided by user 
    private Map<Handler, MediaController.Callback> mClientCallbacks = new HashMap<Handler, MediaController.Callback>();
    
    /**
     * Return the MusicControllerService on bind
     * @return MusicControllerService instance
     */
    public class MusicControllerBinder extends Binder{
        public MusicControllerService getService(){
            return MusicControllerService.this;
        }
    }
    
    /**
     * Returns the active media controller
     * @return MediaController
     */
    public MediaController getMediaController(){
        return mActiveMediaController;
    }
    
    /**
     * Returns the TransportControls for the active media controller
     * @return MediaController.TransportControls
     */
    public MediaController.TransportControls getMediaTransport(){
        return mActiveMediaTransport;
    }

    /**
     * Returns a list of active sessions we have registered
     * @return List of available sessions
     */
    public String[] getMediaSessions(){
        String[] sessionNames = new String[mMediaControllers.size()];
        int index = 0;
        for (MediaController controller : mMediaControllers.values()){
            sessionNames[index] = controller.getPackageName();
            index++;
        }
        return sessionNames;
    }
    
    /**
     * Convenience method for getActiveMediaTransport()
     * @return MediaController.TransportControls
     */
    public MediaController.TransportControls getRemote(){
        return getMediaTransport();
    }
    
    /**
     * Return the MusicControllerBinder to any
     * activities that bind to this service
     * @return mBinder - The classes Binder
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    /**
     * Register callback with the active media session
     * @param cb Clients callback class implementation
     * @param handle The handle to the clients thread
     */
    public void registerCallback(MediaController.Callback cb, Handler handle){
        mClientCallbacks.put(handle, cb);
        if(mActiveMediaController != null){
            mActiveMediaController.registerCallback(cb, handle);            
        }else{
            Log.e(TAG, CTAG + "Attempted to registerCallback on a null session");
        }
    }
    
    /**
     * Switch the active media session to the given session name 
     * @param sessionName The string name of the session to switch to
     */
    public void setMediaSession(String sessionName){
        MediaController newSession = null;
        String sessionToken = mMediaControllerSessionMap.get(sessionName);
        Log.d(TAG, CTAG + "Session Token " + sessionToken);
        if(sessionToken != null){
            newSession = mMediaControllers.get(sessionToken);
            String oldSessionToken = "";
            if(mActiveMediaController != null){
                oldSessionToken = mActiveMediaController.getSessionToken().toString();
            }
            if(newSession != null && oldSessionToken != sessionToken){
                Log.d(TAG, CTAG + "Switching to session " + sessionName);
                for(Handler handle: mClientCallbacks.keySet()){
                    MediaController.Callback cb = mClientCallbacks.get(handle);
                    if(mActiveMediaController != null){
                        mActiveMediaController.unregisterCallback(cb);
                    }
                    newSession.registerCallback(cb, handle);
                }
                mActiveMediaController = newSession;
                mActiveMediaTransport = newSession.getTransportControls();
                Log.d(
                    TAG,
                    String.format(
                        CTAG + "Session set to %s", mActiveMediaController.getPackageName()
                    )
                );
            }else{
                
            }

        }else{
            Log.e(TAG, CTAG + "Requested MediaSession not found!");
        }
    }
    
    /**
     * Unregister the given callback from the active Media Controller
     * @param cb Clients callback class implementation
     */
    public void unregisterCallback(MediaController.Callback cb){
        Log.d(TAG, CTAG + "Unregistering Callback via unregisterCallback()");
        mClientCallbacks.values().remove(cb);
        if(mActiveMediaController != null){
            mActiveMediaController.unregisterCallback(cb);
        }
    }
    
    /**
     * Return the media session manager from the system
     * @return MediaSessionManager
     */
    private MediaSessionManager getMediaSessionManager(){
        return (MediaSessionManager) mContext.getSystemService(
            Context.MEDIA_SESSION_SERVICE
        );
    }
    
    /**
     * Query the system for actively registered MediaSessions
     * and store them for future use
     */
    private void refreshMediaControllers(){
        MediaSessionManager mediaManager = getMediaSessionManager();
        setMediaControllers(mediaManager.getActiveSessions(
            new ComponentName(mContext, MusicControllerService.class)
        ));
    }
    
    /**
     * Send the ACTION_UP and ACTION_DOWN key events
     * @param keyCode The key we are pressing
     */
    private void sendKeyEvent(int keyCode){
        KeyEvent keyDown  = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
        KeyEvent keyUp  = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
        
        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyDown);
        
        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyUp);
        
        mContext.sendOrderedBroadcast(downIntent, null);
        mContext.sendOrderedBroadcast(upIntent, null);
    }
    
    /**
     * Store and report the MediaControllers found
     * @param mediaControllers List of MediaControllers to store
     */
    private void setMediaControllers(List<MediaController> mediaControllers){
        Log.d(TAG, CTAG + "Sessions Changed");
        List<String> currentSessions = new ArrayList<String>();
        for (MediaController remote: mediaControllers){
            String sToken = remote.getSessionToken().toString();
            String sName = remote.getPackageName();
            currentSessions.add(sToken);
            if(!mMediaControllers.containsKey(sToken)){
                mMediaControllers.put(sToken, remote);
                mMediaControllerSessionMap.put(sName, sToken);
            }
            Log.i(
                TAG,
                String.format(
                    CTAG + "Found MediaSession for package %s with state %s and token %s",
                    remote.getPackageName(),
                    remote.getPlaybackState(),
                    sToken
                )
            );
        }
        // Remove Controllers that aren't active
        for(String sToken: mMediaControllers.keySet()){
            if(!currentSessions.contains(sToken)){
                mMediaControllers.remove(sToken);
                mMediaControllerSessionMap.values().remove(sToken);
            }
        }
        // Default to a session if only one exists
        if(
            (mActiveMediaController == null && ! mMediaControllers.isEmpty())
            || 
            mMediaControllers.size() == 1
        ){
            setMediaSession(
                mMediaControllers.get(currentSessions.get(0)).getPackageName()
            );
        }
        // Make sure if there's a session playing
        // that we set it to be the active session
        for(MediaController remote: mMediaControllers.values()){
            PlaybackState ps = remote.getPlaybackState();
            if(ps != null){
                if(ps.getState() == PlaybackState.STATE_PLAYING){
                    Token tk = remote.getSessionToken();
                    if(tk != mActiveMediaController.getSessionToken()){
                        setMediaSession(
                            mMediaControllers.get(tk.toString()).getPackageName()
                        );
                    }
                }
            }
        }
    }
    
    @Override
    public void onActiveSessionsChanged(List<MediaController> mediaControllers){
        Log.i(TAG, CTAG + "System MediaSessions changed");
        setMediaControllers(mediaControllers);
    }
    
    @Override
    public void onCreate(){
        Log.d(TAG, CTAG + "onCreate()");
        // Saving the context for further reuse
        mContext = getApplicationContext();
        // Get the session manager and register
        // us to listen for new sessions
        getMediaSessionManager().addOnActiveSessionsChangedListener(
            this, 
            new ComponentName(mContext, MusicControllerService.class)
        );
        // Get the current active media sessions
        refreshMediaControllers();
        // Handle no active sessions
        if(mMediaControllers.size() == 0){
            sendKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        }
    }
    
    @Override
    public void onDestroy(){
        super.onDestroy();
        Log.d(TAG, CTAG + "onDestroy()");
        // Unregister ALL callbacks
        for(MediaController.Callback cb: mClientCallbacks.values()){
            unregisterCallback(cb);
        }
    }
}