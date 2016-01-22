/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package leap.web;

import java.io.*;
import java.util.Map;

import javax.servlet.*;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import leap.core.AppContext;
import leap.core.AppException;
import leap.core.RequestContext;
import leap.core.web.RequestBase;
import leap.core.web.RequestIgnore;
import leap.core.web.ResponseBase;
import leap.lang.New;
import leap.lang.http.HTTP;
import leap.lang.io.IO;
import leap.lang.logging.Log;
import leap.lang.logging.LogFactory;
import leap.lang.servlet.Servlets;
import leap.web.assets.AssetHandler;
import leap.web.exception.RequestHandledException;
import leap.web.exception.ResponseException;

public class AppFilter implements Filter {
	
	private static final Log log = LogFactory.get(AppFilter.class);
	
	protected ServletContext  servletContext;
	protected AppBootstrap    bootstrap;
	protected App			  app;
	protected AppHandler	  appHandler;
	protected AppContext	  appContext;
	protected AssetHandler	  assetHandler;
	protected RequestIgnore[] ignores;
	
	@Override
    public void init(FilterConfig config) throws ServletException {
		this.servletContext = config.getServletContext();
		this.bootstrap      = AppBootstrap.tryGet(servletContext);
		
		if(null == bootstrap) {
			bootstrap = new AppBootstrap();
			
			Map<String, String> params = New.hashMap();
			params.putAll(Servlets.getInitParamsMap(this.servletContext));
			params.putAll(Servlets.getInitParamsMap(config));
			
		    bootstrap.bootApplication(config.getServletContext(), params);
		}
	    
	    //get beans
	    this.app		  = bootstrap.getApp();
	    this.appHandler   = bootstrap.getAppHandler();
	    this.appContext   = bootstrap.getAppContext();
	    this.assetHandler = bootstrap.getBeanFactory().tryGetBean(AssetHandler.class);
	    this.ignores      = bootstrap.getBeanFactory().getBeans(RequestIgnore.class).toArray(new RequestIgnore[]{});

	    //start application
	    bootstrap.startApplication();
    }

	@Override
    public void doFilter(ServletRequest req, ServletResponse resp, final FilterChain chain) throws IOException, ServletException {
        final RequestWrapper requestWrapper = new RequestWrapper((HttpServletRequest)req);

		final DefaultResponse response = createResponse((HttpServletResponse)resp);
		final DefaultRequest  request  = createRequest(requestWrapper, response);
		
		response.setRequest(request);
		AppContext.setCurrent(appContext);
		RequestContext.setCurrent(request);
		try {
			//handle assets request
			if (null != assetHandler && assetHandler.matches(request)) {
				if (handleAssetRequest(request, response)) {
					return;
				}
			}

			//handle other request
			if (app.filters().isEmpty()) {
				checkIgnoreAndDoService(request, response, chain);
			} else {
				//execute filters first
				FilterChainBase chain1 =
						new FilterChainBase(app.filters()) {
							@Override
							protected void doNext(RequestBase requestBase, ResponseBase responseBase) throws ServletException, IOException {
								checkIgnoreAndDoService(request, response, chain);
							}
						};

				chain1.doFilter(request, response);
			}
		}catch(RequestHandledException e){
			log.debug("Caught a RequestHandledException by filter, stop handle request.");
		}catch(Throwable e){
			boolean handled = false;
			
			try {
				if(e instanceof ResponseException) {
					appHandler.renderResponseException(request, response, (ResponseException)e);
					handled = true;
				}else{
					handled = appHandler.handleError(request, response, e);	
				}
            } catch (Throwable e1) {
            	servletContext.log("Error handling exception by app handler", e1);
            }
			
			if(!handled){
				if(response.isHandled() || response.isCommitted()){
				    //TODO : 
					servletContext.log(e.getMessage(),e);
					response.getWriter().write(e.getMessage());
					e.printStackTrace(response.getWriter());
				}else{
					if(e instanceof RuntimeException) {
						throw (RuntimeException)e;
					}else{
						throw new AppException(e.getMessage(),e);	
					}
				}
			}
		}finally{
            requestWrapper.destroy();
			RequestContext.removeCurrent();
			AppContext.removeCurrent();
		}
    }
	
	protected boolean handleAssetRequest(Request request,Response response) throws ServletException,IOException {
		try {
	        return assetHandler.handle(request, response);
		} catch (RuntimeException e){
			throw e;
        } catch (Throwable e) {
        	throw new ServletException("Error handling assets request, " + e.getMessage(), e);
        }
	}
	
	protected void checkIgnoreAndDoService(Request request,Response response,FilterChain chain) throws ServletException,IOException {
		if(checkIgnore(request, response)){
			log.debug("Request '{}' ignored", request.getPath());
			handleIgnoredRequest(request, response, chain);
			return;
		}
		doService(request, response, chain);
	}
	
	protected boolean checkIgnore(Request request,Response response) throws ServletException, IOException {
		for(int i=0;i<ignores.length;i++){
			if(ignores[i].matches(request)){
				return true;
			}
		};
		return false;
	}
	
	protected void handleIgnoredRequest(Request request,Response response,FilterChain chain) throws ServletException,IOException {
		ErrorServletResponseWrapper espw = new ErrorServletResponseWrapper(response.getServletResponse());
		
		chain.doFilter(request.getServletRequest(), espw);
		
		if(espw.getErrorStatus() == HTTP.SC_NOT_FOUND) {

			doService(request, response, chain);
			
		}else if(espw.isError()){
			espw.commitError();
		}
	}
	
	protected void doService(Request request,Response response,FilterChain chain) throws ServletException, IOException {
		try {
			//init the request
			appHandler.prepareRequest(request, response);
			
			//service the request
	        if(!appHandler.handleRequest(request,response)){
	        	ErrorServletResponseWrapper espw = new ErrorServletResponseWrapper(response.getServletResponse());
	        	
	        	chain.doFilter(request.getServletRequest(), espw);
	        	
	        	if(espw.isError()) {
	        		if(!appHandler.handleError(request, response, espw.getErrorStatus(), espw.getErrorMessage())) {
	        			espw.commitError();
	        		}
	        	}
	        }
		} catch (RuntimeException e){
			throw e;
        } catch (Throwable e) {
        	throw new ServletException("Error servicing current request, " + e.getMessage(), e);
        }
	}
	
    protected DefaultResponse createResponse(HttpServletResponse servletResponse) throws IOException {
	    return new DefaultResponse(servletResponse);
    }

    protected DefaultRequest createRequest(RequestWrapper servletRequest, Response response) throws IOException {
		final DefaultRequest request = new DefaultRequest(app,appHandler,servletRequest,response);
	    request.setCharacterEncoding(app.getDefaultCharset().name());
	    return request;
    }
	
	@Override
    public void destroy() {
		bootstrap.stopApplication();
    }

    protected static class ServletInputStreamWrapper extends ServletInputStream {

        private final InputStream in;

        public ServletInputStreamWrapper(InputStream in) {
            this.in = in;
        }

        @Override
        public boolean isFinished() {
            try{
                return in.available() > 0;
            }catch(IOException e) {
                throw new IllegalStateException("I/O Error : " + e.getMessage(), e);
            }
        }

        @Override
        public boolean isReady() {
            return !isFinished();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            throw new IllegalStateException("setReadListener not supported");
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }
    }
	
	protected class ErrorServletResponseWrapper extends HttpServletResponseWrapper {
		
		private final HttpServletResponse wrapped;
		
		private int    status;
		private String message;
		
		public ErrorServletResponseWrapper(HttpServletResponse response) {
	        super(response);
	        this.wrapped = response;
        }

		@Override
        public void sendError(int sc, String msg) throws IOException {
			e(sc,msg);
		}

		@Override
        public void sendError(int sc) throws IOException {
			e(sc);
		}

		@Override
        public void setStatus(int sc) {
			if(sc >= 400) {
				e(sc);
			}else{
				super.setStatus(sc);	
			}
        }

		@Override
		@Deprecated
        public void setStatus(int sc, String sm) {
			if(sc >= 400) {
				e(sc,sm);
			}else{
				super.setStatus(sc, sm);	
			}
        }
		
		public int getErrorStatus() {
			return status;
		}
		
		public String getErrorMessage() {
			return message;
		}
		
		public boolean isError() {
			return status > 0;
		}
		
		public void commitError() throws IOException {
			wrapped.sendError(status, message);
		}
		
		private void e(int sc) {
			this.status = sc;
		}
		
		private void e(int sc,String sm) {
			this.status  = sc;
			this.message = sm;
		}
	}
}