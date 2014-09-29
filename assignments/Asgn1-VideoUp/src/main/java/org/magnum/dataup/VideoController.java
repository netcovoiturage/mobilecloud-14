package org.magnum.dataup;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class VideoController {

    private static final AtomicLong currentId = new AtomicLong(0L);

    private Map<Long,Video> videos = new HashMap<Long, Video>();

    public Video getVideo(long id){
    	Video v = videos.get(id);
		if(v==null){
			throw new ResourceNotFoundException();
		}
		return v;
    }
    
    public Video save(Video entity) {
        checkAndSetId(entity);
        entity.setDataUrl(getDataUrl(entity.getId()));
        videos.put(entity.getId(), entity);
        return entity;
    }

    private void checkAndSetId(Video entity) {
        if(entity.getId() == 0){
            entity.setId(currentId.incrementAndGet());
        }
    }
    
    private String getDataUrl(long videoId){
        String url = getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
        return url;
    }

    private String getUrlBaseForLocalServer() {
       HttpServletRequest request = 
           ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
       String base = 
          "http://"+request.getServerName() 
          + ((request.getServerPort() != 80) ? ":"+request.getServerPort() : "");
       return base;
    }
    
    // Initialize this member variable somewhere with 
    // videoDataMgr = VideoFileManager.get()
    //
    private VideoFileManager videoDataMgr;

    // You would need some Controller method to call this...
    public void saveSomeVideo(Video v, MultipartFile videoData) throws IOException {
         videoDataMgr.saveVideoData(v, videoData.getInputStream());
    }

    public void serveSomeVideo(Video v, HttpServletResponse response) throws IOException {
         // Of course, you would need to send some headers, etc. to the
         // client too!
         //  ...
         videoDataMgr.copyVideoData(v, response.getOutputStream());
    }
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.POST)
	public @ResponseBody Video addVideo(@RequestBody Video v){
		return save(v);
	} 
	
	@RequestMapping(value=VideoSvcApi.VIDEO_SVC_PATH, method=RequestMethod.GET)
	public @ResponseBody Collection<Video> getVideoList(){
		return videos.values();
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.POST)
	public @ResponseBody VideoStatus setVideoData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, 
			@RequestParam(VideoSvcApi.DATA_PARAMETER) MultipartFile videoData) throws IOException{
		Video v = getVideo(id);
		videoDataMgr = VideoFileManager.get();
		saveSomeVideo(v, videoData);
		return new VideoStatus(VideoStatus.VideoState.READY);
	}
	
	@RequestMapping(value=VideoSvcApi.VIDEO_DATA_PATH, method=RequestMethod.GET)
	public void getData(@PathVariable(VideoSvcApi.ID_PARAMETER) long id, HttpServletResponse response) throws IOException{
		Video v = getVideo(id);
		response.addHeader("Content-Type", v.getContentType());
		videoDataMgr = VideoFileManager.get();
		serveSomeVideo(v, response);
	}
}
