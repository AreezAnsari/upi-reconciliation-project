// import { useState } from 'react';
// import { motion } from 'framer-motion';
// import { Plus } from 'lucide-react';
// import { Button, Input, Select, RadioGroup, Card } from '../../../components/common';
// import { ROLES, PROCESS_TYPES, FILE_TYPES, DUMMY_MENUS } from '../../../config';
// import { useAppStore } from '../../../store';
// import styles from './AddMenu.module.css';

// const MENU_TYPE_OPTIONS = [
//   { id: 'master', label: 'Master Menu' },
//   { id: 'main', label: 'Main Menu' },
//   { id: 'sub', label: 'Sub Menu' },
// ];

// const AddMenu = () => {
//   const { addNotification } = useAppStore();
//   const [formData, setFormData] = useState({
//     menuType: 'master',
//     menuName: '',
//     description: '',
//     role: '',
//     parentMasterMenu: '',
//     processType: '',
//     fileName: '',
//   });
//   const [loading, setLoading] = useState(false);

// const masterMenus = DUMMY_MENUS.master.map(m => ({    id: m.id,
//     label: m.name,
//   }));

//   const handleSubmit = async (e) => {
//     e.preventDefault();
//     setLoading(true);

//     // Simulate API call
//     await new Promise(resolve => setTimeout(resolve, 1000));

//     addNotification({
//       type: 'success',
//       title: 'Menu Added',
//       message: `${formData.menuName} has been added successfully.`,
//     });

//     // Reset form
//     setFormData({
//       menuType: 'master',
//       menuName: '',
//       description: '',
//       role: '',
//       parentMasterMenu: '',
//       processType: '',
//       fileName: '',
//     });

//     setLoading(false);
//   };

//   return (
//     <div className={styles.page}>
//       <motion.div
//         initial={{ opacity: 0, y: -20 }}
//         animate={{ opacity: 1, y: 0 }}
//         className={styles.header}
//       >
//         <h1>Add Menu</h1>
//         <p>Create a new menu item for the application</p>
//       </motion.div>

//       <motion.div
//         initial={{ opacity: 0, y: 20 }}
//         animate={{ opacity: 1, y: 0 }}
//         transition={{ delay: 0.1 }}
//       >
//         <Card className={styles.formCard}>
//           <form onSubmit={handleSubmit}>
//             <div className={styles.menuTypes}>
//               <RadioGroup
//                 name="menuType"
//                 options={MENU_TYPE_OPTIONS}
//                 value={formData.menuType}
//                 onChange={(value) => setFormData({ ...formData, menuType: value })}
//               />
//             </div>

//             <div className={styles.formGrid}>
//               <Input
//                 label="Menu Name"
//                 placeholder="Enter menu name"
//                 value={formData.menuName}
//                 onChange={(e) => setFormData({ ...formData, menuName: e.target.value })}
//                 required
//               />

//               <Input
//                 label="Description"
//                 placeholder="Enter description"
//                 value={formData.description}
//                 onChange={(e) => setFormData({ ...formData, description: e.target.value })}
//               />

//               <Select
//                 label="Select Role"
//                 placeholder="Select a role"
//                 options={ROLES}
//                 value={formData.role}
//                 onChange={(e) => setFormData({ ...formData, role: e.target.value })}
//                 required
//               />

//               {(formData.menuType === 'main' || formData.menuType === 'sub') && (
//                 <Select
//                   label="Select Parent Master Menu"
//                   placeholder="Select an option"
//                   options={masterMenus}
//                   value={formData.parentMasterMenu}
//                   onChange={(e) => setFormData({ ...formData, parentMasterMenu: e.target.value })}
//                   required
//                 />
//               )}

//               {formData.menuType === 'sub' && (
//                 <>
//                   <Select
//                     label="Select Process Type"
//                     placeholder="Select an option"
//                     options={PROCESS_TYPES}
//                     value={formData.processType}
//                     onChange={(e) => setFormData({ ...formData, processType: e.target.value })}
//                   />

//                   <Select
//                     label="Select File Name"
//                     placeholder="Select an option"
//                     options={FILE_TYPES}
//                     value={formData.fileName}
//                     onChange={(e) => setFormData({ ...formData, fileName: e.target.value })}
//                   />
//                 </>
//               )}
//             </div>

//             <div className={styles.actions}>
//               <Button
//                 type="submit"
//                 variant="gold"
//                 loading={loading}
//                 leftIcon={<Plus size={18} />}
//               >
//                 Add Menu
//               </Button>
//             </div>
//           </form>
//         </Card>
//       </motion.div>
//     </div>
//   );
// };

// export default AddMenu;


import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus } from 'lucide-react';
import { Button, Input, Select, Card } from '../../../components/common';
import Stepper from '../../../components/common/Stepper';
import { useAppStore, useAuthStore } from '../../../store';
import { authAPI } from '../../../services';
import styles from './AddMenu.module.css';

const MENU_TYPE_STEPS = [
  { id: 'master', label: 'Master Menu', description: 'Top level menu' },
  { id: 'main', label: 'Main Menu', description: 'Parent category' },
  { id: 'sub', label: 'Sub Menu', description: 'Child item' },
];

const AddMenu = () => {
  const { addNotification, triggerMenuRefresh } = useAppStore();
  const { token, user } = useAuthStore();
  const [formData, setFormData] = useState({
    menuType: 'master',
    menuName: '',
    description: '',
    role: '',
    parentMasterMenu: '',
    parentMainMenu: '',
    processType: '',
    fileName: '',
  });
  const [loading, setLoading] = useState(false);
  const [roles, setRoles] = useState([]);
  const [rolesData, setRolesData] = useState([]);
  const [processTypesData, setProcessTypesData] = useState([]);

  // Get master menus based on selected role
  const masterMenus = (() => {
    if (!formData.role) return [];
    const selectedRole = rolesData.find(r => r.roleId === Number(formData.role));
    if (!selectedRole || !selectedRole.menu) return [];
    return selectedRole.menu
      .filter(m => m.menuType === 'Master')
      .map(m => ({
        id: m.menuId,
        label: m.menuName,
      }));
  })();

  // Get main menus based on selected role and parent master menu
  const mainMenus = (() => {
    if (!formData.role || !formData.parentMasterMenu) return [];
    const selectedRole = rolesData.find(r => r.roleId === Number(formData.role));
    if (!selectedRole || !selectedRole.menu) return [];
    return selectedRole.menu
      .filter(m => m.menuType === 'Main' && m.parentMenuCode === String(formData.parentMasterMenu))
      .map(m => ({
        id: m.menuId,
        label: m.menuName,
      }));
  })();

  // Get process type options from API data
  const processTypeOptions = processTypesData.map(p => ({
    id: p.processMastId,
    label: p.longName,
  }));

  // Get file name options based on selected process type
  const fileNameOptions = (() => {
    if (!formData.processType) return [];
    const selectedProcess = processTypesData.find(p => p.processMastId === Number(formData.processType));
    if (!selectedProcess) return [];

    // If EXTRACTION, use fileList; if RECONCILIATION, use processList
    if (selectedProcess.longName === 'EXTRACTION' && selectedProcess.fileList) {
      return selectedProcess.fileList.map(f => ({
        id: f.reconFileId,
        label: f.reconFileName,
      }));
    } else if (selectedProcess.longName === 'RECONCILIATION' && selectedProcess.processList) {
      return selectedProcess.processList.map(p => ({
        id: p.reconProcessId,
        label: p.reconProcessName,
      }));
    }
    return [];
  })();

  const fetchRoles = async () => {
    try {
      const response = await authAPI.getAllRoles(token);
      if (response.status === 'SUCCESS') {
        setRolesData(response.data);
        const roleOptions = response.data.map(role => ({
          id: role.roleId,
          label: role.roleName,
        }));
        setRoles(roleOptions);
      }
    } catch (error) {
      console.error('Failed to fetch roles:', error);
    }
  };

  const fetchProcessTypes = async () => {
    try {
      const response = await authAPI.getProcess(token);
      if (response.status === 'SUCCESS') {
        setProcessTypesData(response.data);
      }
    } catch (error) {
      console.error('Failed to fetch process types:', error);
    }
  };

  useEffect(() => {
    if (token) {
      fetchRoles();
      fetchProcessTypes();
    }
  }, [token]);

  const handleStepChange = (stepId) => {
    setFormData({ ...formData, menuType: stepId });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      // Map menuType to API values
      const menuTypeMap = {
        master: 'Master',
        main: 'Main',
        sub: 'Submenu',
      };

      // Get the selected process type longName
      const selectedProcess = processTypesData.find(p => p.processMastId === Number(formData.processType));
      const processTypeName = selectedProcess?.longName || ' ';

      // Get selected role's menu data
      const selectedRole = rolesData.find(r => r.roleId === Number(formData.role));
      const roleMenus = selectedRole?.menu || [];

      // Get master menu name
      const selectedMasterMenu = roleMenus.find(m => m.menuId === Number(formData.parentMasterMenu));
      const masterMenuName = selectedMasterMenu?.menuName || ' ';

      // Get main menu name (for submenu)
      const selectedMainMenu = roleMenus.find(m => m.menuId === Number(formData.parentMainMenu));
      const mainMenuName = selectedMainMenu?.menuName || ' ';

      // Get reconFileId from selected file (for menuProcessId)
      let menuProcessId = ' ';
      if (formData.fileName && selectedProcess) {
        // fileName stores the reconFileId, so use it directly
        menuProcessId = String(formData.fileName);
      }

      // Generate menuUrl based on process type for submenu
      let menuUrl = ' ';
      if (formData.menuType === 'sub' && formData.fileName && processTypeName) {
        const reconFileId = formData.fileName;
        if (processTypeName === 'EXTRACTION') {
          menuUrl = `/extraction/fileProcessing.extr?processid=${reconFileId}`;
        } else if (processTypeName === 'RECONCILIATION') {
          menuUrl = `/reconciliation/fileProcessing.extr?processid=${reconFileId}`;
        }
      }

      // Build the API payload
      const payload = {
        menuId: '',
        menuType: menuTypeMap[formData.menuType],
        menuName: formData.menuName,
        menuDescription: formData.description,
        // For submenu: parentMenuCode = main menu name, otherwise master menu ID
        parentMenuCode: formData.menuType === 'sub' ? mainMenuName : (formData.parentMasterMenu || ' '),
        // For submenu: masterMenuParent = master menu name
        masterMenuParent: formData.menuType === 'sub' ? masterMenuName : ' ',
        subMenuReq: ' ',
        menuUrl: menuUrl,
        operations: ' ',
        userId: String(user?.userId || ''),
        roleId: formData.role,
        // For submenu: menuProcessId = reconFileId from selected file
        menuProcessId: formData.menuType === 'sub' ? menuProcessId : ' ',
        processType: processTypeName,
        reconFilePath: formData.fileName || ' ',
      };

      const response = await authAPI.addMenu(payload, token);

      if (response.status === 'SUCCESS') {
        triggerMenuRefresh();
        // Re-fetch roles so dropdowns reflect the newly added menu
        await fetchRoles();
        addNotification({
          type: 'success',
          title: 'Menu Added',
          message: `${formData.menuName} has been added successfully.`,
        });

        // Reset form
        setFormData({
          menuType: 'master',
          menuName: '',
          description: '',
          role: '',
          parentMasterMenu: '',
          parentMainMenu: '',
          processType: '',
          fileName: '',
        });
      } else {
        addNotification({
          type: 'error',
          title: 'Error',
          message: response.statusMsg || 'Failed to add menu.',
        });
      }
    } catch (error) {
      console.error('Failed to add menu:', error);
      addNotification({
        type: 'error',
        title: 'Error',
        message: 'Failed to add menu. Please try again.',
      });
    } finally {
      setLoading(false);
    }
  };

  // Animation variants for form fields
  const formVariants = {
    hidden: { opacity: 0, y: 10 },
    visible: { opacity: 1, y: 0, transition: { duration: 0.3 } },
    exit: { opacity: 0, y: -10, transition: { duration: 0.2 } }
  };

  return (
    <div className={styles.page}>
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className={styles.header}
      >
        <h1>Add Menu</h1>
        <p>Create a new menu item for the application</p>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <Card className={styles.formCard}>
          <form onSubmit={handleSubmit}>
            {/* Stepper for Menu Type Selection */}
            <Stepper
              steps={MENU_TYPE_STEPS}
              currentStep={formData.menuType}
              onStepClick={handleStepChange}
            />

            {/* Form Fields */}
            <AnimatePresence mode="wait">
              <motion.div
                key={formData.menuType}
                variants={formVariants}
                initial="hidden"
                animate="visible"
                exit="exit"
                className={styles.formGrid}
              >
                {/* Common Fields for All Menu Types */}
                <Input
                  label="Menu Name"
                  placeholder="Enter menu name"
                  value={formData.menuName}
                  onChange={(e) => setFormData({ ...formData, menuName: e.target.value })}
                  required
                />

                <Input
                  label="Description"
                  placeholder="Enter description"
                  value={formData.description}
                  onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                />

                <Select
                  label="Select Role"
                  placeholder="Select a role"
                  options={roles}
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: e.target.value, parentMasterMenu: '' })}
                  required
                />

                {/* Additional Fields for Main Menu */}
                {formData.menuType === 'main' && (
                  <Select
                    label="Select Parent Master Menu"
                    placeholder="Select an option"
                    options={masterMenus}
                    value={formData.parentMasterMenu}
                    onChange={(e) => setFormData({ ...formData, parentMasterMenu: e.target.value })}
                    required
                  />
                )}

                {/* Additional Fields for Sub Menu */}
                {formData.menuType === 'sub' && (
                  <>
                    <Select
                      label="Select Parent Master Menu"
                      placeholder="Select an option"
                      options={masterMenus}
                      value={formData.parentMasterMenu}
                      onChange={(e) => setFormData({ ...formData, parentMasterMenu: e.target.value, parentMainMenu: '' })}
                      required
                    />

                    {formData.parentMasterMenu && (
                      <Select
                        label="Select Parent Main Menu"
                        placeholder="Select an option"
                        options={mainMenus}
                        value={formData.parentMainMenu}
                        onChange={(e) => setFormData({ ...formData, parentMainMenu: e.target.value })}
                      />
                    )}

                    <Select
                      label="Select Process Type"
                      placeholder="Select an option"
                      options={processTypeOptions}
                      value={formData.processType}
                      onChange={(e) => setFormData({ ...formData, processType: e.target.value, fileName: '' })}
                    />

                    <Select
                      label="Select File Name"
                      placeholder="Select an option"
                      options={fileNameOptions}
                      value={formData.fileName}
                      onChange={(e) => setFormData({ ...formData, fileName: e.target.value })}
                    />
                  </>
                )}
              </motion.div>
            </AnimatePresence>

            <div className={styles.actions}>
              <Button
                type="submit"
                variant="gold"
                loading={loading}
                leftIcon={<Plus size={18} />}
              >
                Add Menu
              </Button>
            </div>
          </form>
        </Card>
      </motion.div>
    </div>
  );
};

export default AddMenu;
