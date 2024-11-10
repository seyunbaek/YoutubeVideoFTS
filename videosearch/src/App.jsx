import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Link, useNavigate } from 'react-router-dom';
import { Menu, Search, Edit2, Trash2, RefreshCw, ArrowLeft, Clock } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const Logo = ({ className = "text-4xl", onClick }) => (
  <div 
    className={`font-semibold ${className} ${onClick ? 'cursor-pointer' : ''}`}
    onClick={onClick}
  >
    <span className="text-blue-500">V</span>
    <span className="text-red-500">i</span>
    <span className="text-yellow-500">d</span>
    <span className="text-blue-500">e</span>
    <span className="text-green-500">o</span>
    <span className="text-red-500">Search</span>
  </div>
);

const HomePage = ({ searchQuery, setSearchQuery, handleSearch, isSearching }) => {
  const navigate = useNavigate();
  
  const onSubmit = async (e) => {
    e.preventDefault();
    await handleSearch(e);
    navigate('/search');
  };

  return (
    <div className="flex flex-col min-h-screen">
      <div className="absolute top-4 left-4">
        <Button 
          variant="ghost" 
          size="icon"
          onClick={() => navigate('/channels')}
        >
          <Menu className="h-6 w-6" />
        </Button>
      </div>
      
      <div className="flex-1 flex flex-col items-center justify-center -mt-32">
        <div className="mb-8">
          <Logo />
        </div>

        <div className="w-full max-w-xl px-4">
          <form onSubmit={onSubmit}>
            <div className="relative">
              <Input
                type="text"
                placeholder="Search transcripts..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full h-12 px-6 rounded-full border-2 hover:shadow-md focus:shadow-md transition-shadow"
              />
              <Button 
                type="submit"
                variant="ghost"
                size="icon"
                className="absolute right-2 top-1/2 -translate-y-1/2"
                disabled={isSearching}
              >
                <Search className="h-5 w-5 text-gray-500" />
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

const SearchResultsPage = ({ 
  searchQuery, 
  setSearchQuery, 
  handleSearch, 
  searchResults, 
  isSearching,
  formatUploadDate,
  highlightSearchTerm 
}) => {
  const navigate = useNavigate();
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 10;

  const totalResults = searchResults.length;
  const totalPages = Math.ceil(totalResults / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const currentResults = searchResults.slice(startIndex, endIndex);

  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
    window.scrollTo(0, 0);
  };

  const resetSearch = () => {
    setSearchQuery('');
    navigate('/');
  };

  return (
    <div className="max-w-4xl mx-auto">
      <div className="sticky top-0 bg-white border-b">
        <div className="flex items-center gap-4 p-4">
          <Button 
            variant="ghost" 
            size="icon"
            onClick={resetSearch}
          >
            <ArrowLeft className="h-6 w-6" />
          </Button>

          <div className="mr-4">
            <Logo className="text-xl" onClick={resetSearch} />
          </div>

          <div className="flex-1 flex gap-2">
            <Input
              type="text"
              placeholder="Search transcripts..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="flex-1"
            />
            <Button 
              variant="ghost"
              size="icon"
              onClick={handleSearch}
              disabled={isSearching}
            >
              <Search className="h-5 w-5" />
            </Button>
          </div>
        </div>
      </div>

      <div className="p-4">
        <div className="text-sm text-gray-500 mb-4">
          Found matches in {totalResults} videos
        </div>
        <div className="space-y-8">
          {currentResults.map((video) => (
            <div key={video.videoID} className="flex gap-4">
              <div className="w-80">
                <a 
                  href={`https://youtu.be/${video.videoID}`} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="block rounded-lg overflow-hidden hover:opacity-90 transition-opacity"
                >
                  <img 
                    src={video.thumbnailURL} 
                    alt={video.title}
                    className="w-full aspect-video object-cover"
                  />
                </a>
              </div>
              <div className="flex-1">
                <h3 className="text-xl text-blue-600 hover:underline">
                  <a 
                    href={`https://youtu.be/${video.videoID}`}
                    target="_blank" 
                    rel="noopener noreferrer"
                  >
                    {video.title}
                  </a>
                </h3>
                <p className="text-sm text-gray-600 mb-2">
                  {video.channelName} • {formatUploadDate(video.uploadDate)}
                </p>
                <div className="space-y-2">
                  {video.matches.map((match) => (
                    <div key={match.transcriptionID} className="group">
                      <a 
                        href={`https://youtu.be/${video.videoID}?t=${match.startTime}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex items-center text-sm text-gray-500 hover:text-blue-500 mb-1"
                      >
                        <Clock className="h-4 w-4 mr-1" />
                        {Math.floor(match.startTime / 60)}:{(match.startTime % 60).toString().padStart(2, '0')}
                      </a>
                      <p className="text-sm text-gray-700">
                        {highlightSearchTerm(match.text, searchQuery)}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>

        {totalPages > 1 && (
          <div className="flex justify-center items-center gap-2 mt-8">
            <Button
              variant="outline"
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage === 1}
            >
              Previous
            </Button>
            
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((pageNum) => (
              <Button
                key={pageNum}
                variant={currentPage === pageNum ? "default" : "outline"}
                onClick={() => handlePageChange(pageNum)}
                className="w-10 h-10"
              >
                {pageNum}
              </Button>
            ))}
            
            <Button
              variant="outline"
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
            >
              Next
            </Button>
          </div>
        )}
      </div>
    </div>
  );
};

const SubscriptionListPage = () => {
  const navigate = useNavigate();
  const [subscriptions, setSubscriptions] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [refreshingChannels, setRefreshingChannels] = useState(new Set());
  const [editingChannel, setEditingChannel] = useState(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [newChannel, setNewChannel] = useState({ name: '', url: '' });

  useEffect(() => {
    fetchChannels();
  }, []);

  const fetchChannels = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/channels');
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      const data = await response.json();
      setSubscriptions(data);
    } catch (error) {
      console.error('Error fetching channels:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefreshAll = async () => {
    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/channels/refresh-all', {
        method: 'POST',
      });
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      await fetchChannels();
    } catch (error) {
      console.error('Error refreshing all channels:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleRefreshChannel = async (channelId) => {
    setRefreshingChannels(prev => new Set([...prev, channelId]));
    try {
      const response = await fetch(`http://localhost:8080/api/channels/refresh/${channelId}`, {
        method: 'POST',
      });
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      await fetchChannels();
    } catch (error) {
      console.error('Error refreshing channel:', error);
    } finally {
      setRefreshingChannels(prev => {
        const next = new Set(prev);
        next.delete(channelId);
        return next;
      });
    }
  };

  const handleEditChannel = (channel) => {
    setEditingChannel({
      ...channel,
      newName: channel.channelName,
      newUrl: channel.URL
    });
  };

  const handleSaveChannel = async () => {
    if (!editingChannel) return;
    
    setIsLoading(true);
    try {
      // First update the channel info
      const updateResponse = await fetch(`http://localhost:8080/api/channels/${editingChannel.channelID}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          channelName: editingChannel.newName,
          URL: editingChannel.newUrl,
        }),
      });
      
      if (!updateResponse.ok) throw new Error(`HTTP error! status: ${updateResponse.status}`);
      
      // Then refresh the channel to update DB
      const refreshResponse = await fetch(`http://localhost:8080/api/channels/refresh/${editingChannel.channelID}`, {
        method: 'POST',
      });

      if (!refreshResponse.ok) throw new Error(`HTTP error! status: ${refreshResponse.status}`);
      
      await fetchChannels();
      setEditingChannel(null);
    } catch (error) {
      console.error('Error updating channel:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleDeleteChannel = async (channelId) => {
    const channel = subscriptions.find(s => s.channelID === channelId);
    const confirmMessage = 
      `Are you sure you want to delete ${channel.channelName}?\n\n` +
      `This will permanently delete:\n` +
      `- All ${channel.numVideos} video transcriptions\n` +
      `- All indexed search data\n` +
      `- Channel information\n\n` +
      `This cannot be undone.`;

    if (!window.confirm(confirmMessage)) return;

    setIsLoading(true);
    try {
      const response = await fetch(`http://localhost:8080/api/channels/${channelId}`, {
        method: 'DELETE',
      });
      
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      await fetchChannels();
    } catch (error) {
      console.error('Error deleting channel:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleAddChannel = async (e) => {
    e.preventDefault();
    if (!newChannel.name.trim() || !newChannel.url.trim()) return;

    setIsLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/channels', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ 
          channelName: newChannel.name,
          URL: newChannel.url 
        }),
      });
      
      if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
      
      setNewChannel({ name: '', url: '' });
      setShowAddModal(false);
      await fetchChannels();
    } catch (error) {
      console.error('Error adding channel:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const addChannelModal = showAddModal ? (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center">
      <Card className="w-[400px] bg-white">
        <CardContent className="p-6">
          <h2 className="text-xl font-semibold mb-4">Add New Channel</h2>
          <form onSubmit={handleAddChannel} className="space-y-4">
            <div className="space-y-2">
              <label htmlFor="channelName" className="text-sm font-medium text-gray-700">
                Channel Name
              </label>
              <Input
                id="channelName"
                value={newChannel.name}
                onChange={(e) => setNewChannel(prev => ({ ...prev, name: e.target.value }))}
                placeholder="Enter channel name"
                disabled={isLoading}
              />
            </div>
            <div className="space-y-2">
              <label htmlFor="channelUrl" className="text-sm font-medium text-gray-700">
                Channel URL
              </label>
              <Input
                id="channelUrl"
                value={newChannel.url}
                onChange={(e) => setNewChannel(prev => ({ ...prev, url: e.target.value }))}
                placeholder="https://www.youtube.com/@channelname"
                disabled={isLoading}
              />
            </div>
            <div className="flex justify-end gap-2 mt-6">
              <Button 
                type="button" 
                variant="ghost"
                onClick={() => {
                  setShowAddModal(false);
                  setNewChannel({ name: '', url: '' });
                }}
                disabled={isLoading}
              >
                Cancel
              </Button>
              <Button 
                type="submit"
                disabled={isLoading || !newChannel.name.trim() || !newChannel.url.trim()}
              >
                {isLoading ? 'Adding...' : 'Add Channel'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  ) : null;

  return (
    <div className="min-h-screen bg-white p-4">
      <Card className="max-w-4xl mx-auto">
        <CardContent className="p-6">
          <div className="flex items-center gap-4 mb-6">
            <Button 
              variant="ghost" 
              size="icon"
              onClick={() => navigate('/')}
            >
              <ArrowLeft className="h-6 w-6" />
            </Button>
            <h2 className="text-xl font-semibold">Channel List</h2>
            <div className="flex-1 flex justify-end gap-2">
              <Button 
                variant="outline"
                size="sm"
                onClick={handleRefreshAll}
                disabled={isLoading}
                className="flex items-center gap-2"
              >
                <RefreshCw className={`h-4 w-4 ${isLoading ? 'animate-spin' : ''}`} />
                {isLoading ? 'Refreshing...' : 'Refresh All'}
              </Button>
              <Button 
                onClick={() => setShowAddModal(true)}
                disabled={isLoading}
              >
                Add Channel
              </Button>
            </div>
          </div>

          {isLoading && subscriptions.length === 0 && (
            <div className="text-center py-4 text-gray-500">
              Loading channels...
            </div>
          )}

          <div className="space-y-2">
            {subscriptions.map((subscription) => (
              <div key={subscription.channelID} className="flex items-center justify-between p-4 border rounded-lg">
                {editingChannel?.channelID === subscription.channelID ? (
                  <div className="flex-1 flex gap-4 items-center">
                    <div className="space-y-2 flex-1">
                      <Input
                        value={editingChannel.newName}
                        onChange={(e) => setEditingChannel({
                          ...editingChannel,
                          newName: e.target.value
                        })}
                        placeholder="Channel name"
                        disabled={isLoading}
                      />
                      <Input
                        value={editingChannel.newUrl}
                        onChange={(e) => setEditingChannel({
                          ...editingChannel,
                          newUrl: e.target.value
                        })}
                        placeholder="Channel URL"
                        disabled={isLoading}
                      />
                    </div>
                    <div className="flex gap-2">
                      <Button 
                        variant="outline" 
                        size="sm" 
                        onClick={handleSaveChannel}
                        disabled={isLoading}
                      >
                        Save
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="sm" 
                        onClick={() => setEditingChannel(null)}
                        disabled={isLoading}
                      >
                        Cancel
                      </Button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div>
                      <h3 className="font-medium">{subscription.channelName}</h3>
                      <p className="text-sm text-gray-500">
                        {subscription.numVideos} videos indexed
                      </p>
                    </div>
                    <div className="flex gap-2">
                      <Button 
                        variant="ghost" 
                        size="icon"
                        onClick={() => handleRefreshChannel(subscription.channelID)}
                        disabled={isLoading || refreshingChannels.has(subscription.channelID)}
                      >
                        <RefreshCw className={`h-4 w-4 ${refreshingChannels.has(subscription.channelID) ? 'animate-spin' : ''}`} />
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="icon"
                        onClick={() => handleEditChannel(subscription)}
                        disabled={isLoading}
                      >
                        <Edit2 className="h-4 w-4" />
                      </Button>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        className="text-red-500"
                        onClick={() => handleDeleteChannel(subscription.channelID)}
                        disabled={isLoading}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
      {addChannelModal}
    </div>
  );
};

const VideoSearchApp = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);
  const [searchResults, setSearchResults] = useState([]);
  const [errorMessage, setErrorMessage] = useState('');

  const formatUploadDate = (dateString) => {
    if (!dateString) return '';
    
    if (dateString.match(/^\d{8}$/)) {
      const year = dateString.substring(0, 4);
      const month = dateString.substring(4, 6);
      const day = dateString.substring(6, 8);
      return new Date(`${year}-${month}-${day}`).toLocaleDateString();
    }
    
    const date = new Date(dateString);
    return date.getTime() ? date.toLocaleDateString() : '';
  };

  const getDateValue = (dateString) => {
    if (!dateString) return 0;
    
    if (dateString.match(/^\d{8}$/)) {
      const year = dateString.substring(0, 4);
      const month = dateString.substring(4, 6);
      const day = dateString.substring(6, 8);
      return new Date(`${year}-${month}-${day}`).getTime();
    }
    
    return new Date(dateString).getTime() || 0;
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;

    setIsSearching(true);
    try {
      const response = await fetch(`http://localhost:8080/api/search?q=${encodeURIComponent(searchQuery)}`, {
        method: 'GET',
      });
      const data = await response.json();
      
      if (data.success) {
        const sortedResults = data.results.sort((a, b) => {
          return getDateValue(b.uploadDate) - getDateValue(a.uploadDate);
        });
        setSearchResults(sortedResults);
      } else {
        setErrorMessage(data.error || 'Search failed');
      }
    } catch (error) {
      console.error('Error performing search:', error);
      setErrorMessage('Failed to perform search');
    } finally {
      setIsSearching(false);
    }
  };

  const highlightSearchTerm = (text, searchTerm) => {
    if (!searchTerm) return text;
    const parts = text.split(new RegExp(`(${searchTerm})`, 'gi'));
    return parts.map((part, i) => 
      part.toLowerCase() === searchTerm.toLowerCase() 
        ? <span key={i} className="bg-yellow-200">{part}</span>
        : part
    );
  };

  return (
    <BrowserRouter>
      <div className="min-h-screen bg-white p-4">
        <Routes>
          <Route 
            path="/" 
            element={
              <HomePage 
                searchQuery={searchQuery}
                setSearchQuery={setSearchQuery}
                handleSearch={handleSearch}
                isSearching={isSearching}
              />
            } 
          />
          <Route 
            path="/search" 
            element={
              <SearchResultsPage 
                searchQuery={searchQuery}
                setSearchQuery={setSearchQuery}
                handleSearch={handleSearch}
                searchResults={searchResults}
                isSearching={isSearching}
                formatUploadDate={formatUploadDate}
                highlightSearchTerm={highlightSearchTerm}
              />
            } 
          />
          <Route 
            path="/channels" 
            element={<SubscriptionListPage />} 
          />
        </Routes>
        {errorMessage && (
          <div className="fixed bottom-4 right-4 bg-red-100 text-red-700 p-4 rounded-lg shadow-lg">
            {errorMessage}
            <button 
              onClick={() => setErrorMessage('')}
              className="ml-4 text-sm underline"
            >
              Dismiss
            </button>
          </div>
        )}
      </div>
    </BrowserRouter>
  );
};

export default VideoSearchApp;