import React from 'react';
import { useNavigate } from 'react-router-dom';
import './Navbar.css';

function Navbar() {
  const navigate = useNavigate();
  const user = JSON.parse(localStorage.getItem('user') || '{}');

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-container">
        <div className="navbar-logo">
          <h1>Achilles Ltd</h1>
        </div>
        <ul className="navbar-menu">
          <li><a href="/dashboard">Dashboard</a></li>
          <li><a href="/documents">Documents</a></li>
          <li><a href="/analysis">Analysis</a></li>
          <li className="navbar-user">
            <span>Welcome, {user.username}</span>
            <button onClick={handleLogout} className="logout-btn">Logout</button>
          </li>
        </ul>
      </div>
    </nav>
  );
}

export default Navbar;
