import { useEffect, useState } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';

export const useIPTracking = (attemptId, isActive = false) => {
    const [ipAddress, setIpAddress] = useState(null);
    const [location, setLocation] = useState(null);
    const [ipChanged, setIpChanged] = useState(false);

    useEffect(() => {
        if (!isActive || !attemptId) return;

        const trackIP = async () => {
            try {
                // Get IP address from a free API
                const response = await fetch('https://api.ipify.org?format=json');
                const data = await response.json();
                const currentIP = data.ip;

                setIpAddress(currentIP);

                // Get stored IP from session storage
                const storedIP = sessionStorage.getItem(`test_ip_${attemptId}`);

                if (storedIP && storedIP !== currentIP) {
                    // IP changed during test - suspicious!
                    setIpChanged(true);
                    await logIPViolation(currentIP, storedIP);
                } else if (!storedIP) {
                    // First time - store IP
                    sessionStorage.setItem(`test_ip_${attemptId}`, currentIP);

                    // Log initial IP
                    await logInitialIP(currentIP);
                }

                // Get approximate location (optional)
                try {
                    const geoResponse = await fetch(`https://ipapi.co/${currentIP}/json/`);
                    const geoData = await geoResponse.json();
                    setLocation({
                        city: geoData.city,
                        region: geoData.region,
                        country: geoData.country_name
                    });
                } catch (err) {
                    console.log('Geolocation unavailable');
                }

            } catch (error) {
                console.error('Failed to track IP:', error);
            }
        };

        trackIP();

        // Check IP every 2 minutes
        const interval = setInterval(trackIP, 120000);

        return () => clearInterval(interval);
    }, [attemptId, isActive]);

    const logInitialIP = async (ip) => {
        try {
            await api.post('/proctor/violation', {
                attemptId,
                eventType: 'IP_TRACKED',
                metadata: {
                    ipAddress: ip,
                    timestamp: new Date().toISOString()
                }
            });
        } catch (error) {
            console.error('Failed to log IP:', error);
        }
    };

    const logIPViolation = async (newIP, oldIP) => {
        try {
            await api.post('/proctor/violation', {
                attemptId,
                eventType: 'IP_ADDRESS_CHANGED',
                metadata: {
                    oldIP,
                    newIP,
                    timestamp: new Date().toISOString()
                }
            });

            toast.error('⚠️ IP address changed - this has been logged as suspicious');
        } catch (error) {
            console.error('Failed to log IP violation:', error);
        }
    };

    return {
        ipAddress,
        location,
        ipChanged
    };
};
