import React from 'react';
import './Footer.css';

function Footer() {
  const currentYear = new Date().getFullYear();
  
  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-section">
          <h3>Achilles Ltd</h3>
          <p>Advanced document analysis and insights</p>
        </div>
        <div className="footer-section">
          <h4>Quick Links</h4>
          <ul>
            <li><a href="/dashboard">Dashboard</a></li>
            <li><a href="/documents">Documents</a></li>
            <li><a href="/analysis">Analysis</a></li>
          </ul>
        </div>
        <div className="footer-section">
          <h4>Contact</h4>
          <p>Email: support@achillesltd.com</p>
          <p>Phone: +353 123 4567</p>
        </div>
      </div>
      <div className="footer-bottom">
        <p>&copy; {currentYear} Achilles Ltd. All rights reserved.</p>
      </div>
    </footer>
  );
}

export default Footer;
