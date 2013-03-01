#!/usr/bin/env python
# This file is part of Amobisense utils.
#
# This file is based on server from Funf Sensing Platform.
# Funf: Open Sensing Framework
# Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
# Acknowledgments: Alan Gardner
# Contact: nadav@media.mit.edu
# 
# Funf is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation, either version 3 of
# the License, or (at your option) any later version.
# 
# Amobisense and all utils is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
# See the GNU Lesser General Public License for more details.
# 
# You should have received a copy of the GNU Lesser General Public
# License along with this file. If not, see <http://www.gnu.org/licenses/>.




from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
from SocketServer import ThreadingMixIn
import sys
import cgi
import urlparse
import os.path
import shutil
import time

server_dir = os.path.dirname(__file__)

config_path = '/config'
config_file_path = os.path.join(server_dir, 'config.json')

upload_path = '/upload'
upload_dir = os.path.join(server_dir, 'uploads')

def read_config():
    config = None
    try:
        with open(config_file_path) as config_file:
            config = config_file.read()
    except IOError:
        pass
    return config

def backup_file(filepath):
    shutil.move(filepath, filepath + '.' + str(int(time.time()*1000)) + '.bak')

def write_file(filename, file):
    if not os.path.exists(upload_dir):
        os.mkdir(upload_dir)
    filepath = os.path.join(upload_dir, filename)
    if os.path.exists(filepath):
        backup_file(filepath)
    with open(filepath, 'wb') as output_file:
        while True:
            chunk = file.read(1024)
            if not chunk:
                break
            output_file.write(chunk)

class RequestHandler(BaseHTTPRequestHandler):
    
    def do_GET(self):
        parsed_url = urlparse.urlparse(self.path)
        if parsed_url.path == config_path:
            config = read_config()
            if config:
                self.send_response(200)
                self.end_headers()
                self.wfile.write(config)
            else:
                self.send_error(500)
        elif parsed_url.path == upload_path:
            self.send_error(405)
        else:
            self.send_error(404)
    
    def do_POST(self):
        parsed_url = urlparse.urlparse(self.path)
        path = parsed_url.path
        ctype, pdict = cgi.parse_header(self.headers['Content-Type']) 
        if path == upload_path:
            if ctype=='multipart/form-data':
                form = cgi.FieldStorage(self.rfile, self.headers, environ={'REQUEST_METHOD':'POST'})
                try:
                    fileitem = form["uploadedfile"]
                    if fileitem.file:
                        try:
                            write_file(fileitem.filename, fileitem.file)
                        except Exception as e:
                            print e
                            self.send_error(500)
                        else:
                            self.send_response(200)
                            self.end_headers()
                            self.wfile.write("OK")
                        return
                except KeyError:
                    pass
            # Bad request
            self.send_error(400)
        elif parsed_url.path == config_path:
            self.send_error(405)
        else:
            self.send_error(404)
        

class ThreadedHTTPServer(ThreadingMixIn, HTTPServer):
    """Handle requests in a separate thread."""                

    
if __name__ == '__main__':
    if sys.argv[1:]:
        port = int(sys.argv[1])
    else:
        port = 8000
    server_address = ('', port)
    httpd = ThreadedHTTPServer(server_address, RequestHandler)

    sa = httpd.socket.getsockname()
    print "Serving HTTP on", sa[0], "port", sa[1], "..."
    print 'use <Ctrl-C> to stop'
    httpd.serve_forever();
    