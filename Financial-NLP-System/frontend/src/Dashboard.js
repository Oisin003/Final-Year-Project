import React, { useEffect, useState } from 'react';
import axios from 'axios';
import Navbar from './Navbar';
import Header from './Header';
import Footer from './Footer';
import './Dashboard.css';

function Dashboard() {
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUserInfo = async () => {
      try {
        const token = localStorage.getItem('token');
        const response = await axios.get('http://localhost:5000/api/me', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });
        setUserInfo(response.data);
      } catch (error) {
        console.error('Error fetching user info:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchUserInfo();
  }, []);

  if (loading) {
    return (
      <div className="page-wrapper">
        <Navbar />
        <Header />
        <div className="dashboard-container">
          <p>Loading...</p>
        </div>
        <Footer />
      </div>
    );
  }

  return (
    <div className="page-wrapper">
      <Navbar />
      <Header />
      
      <main className="dashboard-container">
        <div className="dashboard-content">
          <h2>Welcome to Your Dashboard</h2>
          
          {userInfo && (
            <div className="user-info-card">
              <h3>Account Information</h3>
              <p><strong>Username:</strong> {userInfo.username}</p>
              <p><strong>Email:</strong> {userInfo.email}</p>
              <p><strong>Member Since:</strong> {new Date(userInfo.created_at).toLocaleDateString()}</p>
            </div>
          )}

          <div className="dashboard-grid">
            <div className="dashboard-card">
              <h3>📄 Documents</h3>
              <p>Upload and manage your financial documents</p>
              <button className="card-button">View Documents</button>
            </div>

            <div className="dashboard-card">
              <h3>📊 Analysis</h3>
              <p>View NLP analysis and insights</p>
              <button className="card-button">Run Analysis</button>
            </div>

            <div className="dashboard-card">
              <h3>📈 Reports</h3>
              <p>Generate comprehensive reports</p>
              <button className="card-button">Create Report</button>
            </div>

            <div className="dashboard-card">
              <h3>⚙️ Settings</h3>
              <p>Manage your account settings</p>
              <button className="card-button">View Settings</button>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  );
}

export default Dashboard;
