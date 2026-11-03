// Main layout with collapsible sidebar and top header

import React, { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router';
import {
  Box,
  Drawer,
  AppBar,
  Toolbar,
  Typography,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Divider,
  Avatar,
  Menu,
  MenuItem,
  Tooltip,
  Chip,
  Alert,
} from '@mui/material';
import {
  Menu as MenuIcon,
  ChevronLeft,
  People,
  Domain as DomainIcon,
  PersonAdd,
  Policy as PolicyIcon,
  Science,
  AccountCircle,
  Settings,
  Logout,
  Security,
  CheckCircle,
  Dashboard as DashboardIcon,
  BookmarkBorder,
} from '@mui/icons-material';
import { useAuth } from '../context/AuthContext';
import { domainsApi } from '../api/domains';
import { usersApi } from '../api/users';
import { rolesApi } from '../api/roles';
import type { DomainDto, RoleDto } from '../api/types';

const DRAWER_WIDTH = 240;
const DRAWER_WIDTH_COLLAPSED = 64;

export default function MainLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [profileMenuAnchor, setProfileMenuAnchor] = useState<null | HTMLElement>(null);
  const [allDomains, setAllDomains] = useState<DomainDto[]>([]);
  const [allRoles, setAllRoles] = useState<RoleDto[]>([]);
  const navigate = useNavigate();
  const location = useLocation();
  const { auth, logout, switchDomain } = useAuth();

  // Load accessible domains and roles on mount
  React.useEffect(() => {
    domainsApi.getAll().then(setAllDomains).catch(() => {});
    rolesApi.getAll().then(setAllRoles).catch(() => {});
  }, [auth.currentDomainId]);

  const handleProfileMenuOpen = (event: React.MouseEvent<HTMLElement>) => {
    setProfileMenuAnchor(event.currentTarget);
  };

  const handleProfileMenuClose = () => {
    setProfileMenuAnchor(null);
  };

  const handleLogout = () => {
    handleProfileMenuClose();
    logout();
    navigate('/login');
  };

  const handleDomainSwitch = async (domainId: string) => {
    await switchDomain(domainId);
  };

  const handleSetDefaultDomain = async (domainId: string) => {
    if (!auth.user) return;
    await usersApi.setDefaultDomain(auth.user.id, domainId);
  };

  // Derive current domain name from loaded domains
  const currentDomain = allDomains.find(d => d.id === auth.currentDomainId) ?? null;

  // Derive user accessible domains from auth.user.domainRoles
  const userDomainIds = new Set(auth.user?.domainRoles?.map(dr => dr.domainId) ?? []);
  const userDomains = allDomains.filter(d => userDomainIds.has(d.id));

  // Roles in current domain
  const currentDomainRoles = auth.user?.domainRoles?.find(dr => dr.domainId === auth.currentDomainId)?.roles ?? [];
  const userRoles = currentDomainRoles; // role IDs — display as-is or look up names

  // Permission check: check if user has any roles in current domain
  const hasPermission = (permission: string): boolean => {
    // Admin role (first role in system) has all permissions
    return currentDomainRoles.length > 0; // simplified: any role grants access
  };

  const navigationItems = [
    {
      section: 'Overview',
      items: [
        {
          label: 'Dashboard',
          icon: <DashboardIcon />,
          path: '/',
          permission: null, // Always accessible
        },
      ],
    },
    {
      section: 'Access Control Policies',
      items: [
        {
          label: 'Manage Policies',
          icon: <PolicyIcon />,
          path: '/policies',
          permission: 'manage_policies',
        },
        {
          label: 'Shared Rule Library',
          icon: <BookmarkBorder />,
          path: '/rule-templates',
          permission: 'manage_policies',
        },
        {
          label: 'Test Policies',
          icon: <Science />,
          path: '/test-policies',
          permission: 'test_policies',
        },
      ],
    },
    {
      section: 'Administration',
      items: [
        {
          label: 'Manage User Roles',
          icon: <Security />,
          path: '/roles',
          permission: 'manage_roles',
        },
        {
          label: 'Manage Domains',
          icon: <DomainIcon />,
          path: '/domains',
          permission: 'manage_domains',
        },
        {
          label: 'Manage Users',
          icon: <People />,
          path: '/users',
          permission: 'manage_users',
        },
      ],
    },
    {
      section: 'User Profile',
      items: [
        {
          label: 'Profile',
          icon: <AccountCircle />,
          path: '/profile',
          permission: null, // Always accessible
        },
      ],
    },
    {
      section: 'Application Settings',
      items: [
        {
          label: 'App Settings',
          icon: <Settings />,
          path: '/settings',
          permission: null, // Always accessible
        },
      ],
    },
  ];

  // Filter navigation items based on permissions
  const filteredNavigation = navigationItems
    .map(section => ({
      ...section,
      items: section.items.filter(
        item =>
          !item.permission ||
          (auth.currentDomainId && hasPermission(item.permission))
      ),
    }))
    .filter(section => section.items.length > 0);

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppBar
        position="fixed"
        sx={{
          zIndex: (theme: { zIndex: { drawer: number } }) => theme.zIndex.drawer + 1,
          transition: theme =>
            theme.transitions.create(['width', 'margin'], {
              easing: theme.transitions.easing.sharp,
              duration: theme.transitions.duration.leavingScreen,
            }),
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="toggle sidebar"
            onClick={() => setSidebarOpen(!sidebarOpen)}
            edge="start"
            sx={{ mr: 2 }}
          >
            {sidebarOpen ? <ChevronLeft /> : <MenuIcon />}
          </IconButton>

          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            ABAC Policy Administration Point
          </Typography>

          <Tooltip title="User Profile">
            <IconButton onClick={handleProfileMenuOpen} color="inherit">
              <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main' }}>
                {auth.user?.username[0].toUpperCase()}
              </Avatar>
              <Typography variant="body2" sx={{ ml: 1, display: { xs: 'none', sm: 'block' } }}>
                {auth.user?.username}
              </Typography>
            </IconButton>
          </Tooltip>
        </Toolbar>
      </AppBar>

      <Menu
        anchorEl={profileMenuAnchor}
        open={Boolean(profileMenuAnchor)}
        onClose={handleProfileMenuClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'right',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'right',
        }}
        PaperProps={{
          sx: { minWidth: 300, mt: 1 },
        }}
      >
        <Box sx={{ px: 2, py: 1 }}>
          <Typography variant="subtitle2" color="text.secondary">
            Current Domain
          </Typography>
          {currentDomain ? (
            <Typography variant="body1" sx={{ fontWeight: 600 }}>
              {currentDomain.name}
            </Typography>
          ) : (
            <Alert severity="info" sx={{ mt: 1 }}>
              No domain selected
            </Alert>
          )}
        </Box>

        {userRoles.length > 0 && (
          <Box sx={{ px: 2, py: 1 }}>
            <Typography variant="subtitle2" color="text.secondary" gutterBottom>
              Your Roles
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 0.5 }}>
              {userRoles.map(roleId => (
                <Chip
                  key={roleId}
                  label={allRoles.find(r => r.id === roleId)?.name || roleId}
                  size="small"
                  color="primary"
                  variant="outlined"
                />
              ))}
            </Box>
          </Box>
        )}

        {userDomains.length > 0 && (
          <>
            <Divider sx={{ my: 1 }} />
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Switch Domain
              </Typography>
              {userDomains.map(domain => (
                <MenuItem
                  key={domain.id}
                  onClick={() => {
                    handleDomainSwitch(domain.id);
                    handleProfileMenuClose();
                  }}
                  selected={domain.id === auth.currentDomainId}
                  sx={{ borderRadius: 1, my: 0.5 }}
                >
                  <ListItemIcon>
                    {domain.id === auth.user?.defaultDomainId && (
                      <CheckCircle fontSize="small" color="success" />
                    )}
                  </ListItemIcon>
                  <ListItemText
                    primary={domain.name}
                    secondary={domain.id === auth.user?.defaultDomainId ? 'Default' : null}
                  />
                </MenuItem>
              ))}
            </Box>
            <Box sx={{ px: 2, py: 1 }}>
              <Typography variant="caption" color="text.secondary">
                Set as default domain
              </Typography>
              {userDomains.map(domain => (
                <MenuItem
                  key={`default-${domain.id}`}
                  onClick={() => {
                    handleSetDefaultDomain(domain.id);
                    handleProfileMenuClose();
                  }}
                  disabled={domain.id === auth.user?.defaultDomainId}
                  sx={{ fontSize: '0.875rem', pl: 4 }}
                >
                  {domain.name}
                </MenuItem>
              ))}
            </Box>
          </>
        )}

        {userDomains.length === 0 && (
          <Alert severity="warning" sx={{ m: 2 }}>
            You are not associated with any domains. Contact your administrator.
          </Alert>
        )}

        <Divider />
        <MenuItem
          onClick={() => {
            handleProfileMenuClose();
            navigate('/profile');
          }}
        >
          <ListItemIcon>
            <AccountCircle fontSize="small" />
          </ListItemIcon>
          <ListItemText>View Profile</ListItemText>
        </MenuItem>
        <MenuItem onClick={handleLogout}>
          <ListItemIcon>
            <Logout fontSize="small" />
          </ListItemIcon>
          <ListItemText>Logout</ListItemText>
        </MenuItem>
      </Menu>

      <Drawer
        variant="permanent"
        open={sidebarOpen}
        sx={{
          width: sidebarOpen ? DRAWER_WIDTH : DRAWER_WIDTH_COLLAPSED,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: sidebarOpen ? DRAWER_WIDTH : DRAWER_WIDTH_COLLAPSED,
            boxSizing: 'border-box',
            transition: theme =>
              theme.transitions.create('width', {
                easing: theme.transitions.easing.sharp,
                duration: theme.transitions.duration.enteringScreen,
              }),
            overflowX: 'hidden',
          },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: 'auto', mt: 2 }}>
          {filteredNavigation.map((section, index) => (
            <React.Fragment key={section.section}>
              {index > 0 && <Divider sx={{ my: 1 }} />}
              {sidebarOpen && (
                <Typography
                  variant="caption"
                  sx={{
                    px: 2,
                    py: 1,
                    display: 'block',
                    color: 'text.secondary',
                    fontWeight: 600,
                    textTransform: 'uppercase',
                    letterSpacing: 1,
                  }}
                >
                  {section.section}
                </Typography>
              )}
              <List>
                {section.items.map(item => (
                  <ListItem key={item.path} disablePadding sx={{ display: 'block' }}>
                    <Tooltip title={!sidebarOpen ? item.label : ''} placement="right">
                      <ListItemButton
                        selected={location.pathname === item.path}
                        onClick={() => navigate(item.path)}
                        sx={{
                          minHeight: 48,
                          justifyContent: sidebarOpen ? 'initial' : 'center',
                          px: 2.5,
                        }}
                      >
                        <ListItemIcon
                          sx={{
                            minWidth: 0,
                            mr: sidebarOpen ? 3 : 'auto',
                            justifyContent: 'center',
                          }}
                        >
                          {item.icon}
                        </ListItemIcon>
                        <ListItemText
                          primary={item.label}
                          sx={{ opacity: sidebarOpen ? 1 : 0 }}
                        />
                      </ListItemButton>
                    </Tooltip>
                  </ListItem>
                ))}
              </List>
            </React.Fragment>
          ))}
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        <Outlet />
      </Box>
    </Box>
  );
}