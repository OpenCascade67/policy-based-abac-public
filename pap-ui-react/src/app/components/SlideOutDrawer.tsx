// Reusable slide-out drawer for forms

import React from 'react';
import {
  Drawer,
  Box,
  Typography,
  IconButton,
  Divider,
  Button,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { Close } from '@mui/icons-material';

interface SlideOutDrawerProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  onSave?: () => void;
  saveLabel?: string;
  saveDisabled?: boolean;
}

export default function SlideOutDrawer({
  open,
  onClose,
  title,
  children,
  onSave,
  saveLabel = 'Save',
  saveDisabled = false,
}: SlideOutDrawerProps) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      PaperProps={{
        sx: {
          width: isMobile ? '100%' : { sm: '60%', md: '50%', lg: '40%' },
          maxWidth: '600px',
        },
      }}
    >
      <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="h6">{title}</Typography>
          <IconButton onClick={onClose} edge="end" aria-label="close">
            <Close />
          </IconButton>
        </Box>

        <Divider />

        <Box sx={{ flexGrow: 1, overflow: 'auto', p: 3 }}>{children}</Box>

        {onSave && (
          <>
            <Divider />
            <Box sx={{ p: 2, display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button onClick={onClose} variant="outlined">
                Cancel
              </Button>
              <Button onClick={onSave} variant="contained" disabled={saveDisabled}>
                {saveLabel}
              </Button>
            </Box>
          </>
        )}
      </Box>
    </Drawer>
  );
}
